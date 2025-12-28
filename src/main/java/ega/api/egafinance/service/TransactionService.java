package ega.api.egafinance.service;

import ega.api.egafinance.dto.TransactionInput;
import ega.api.egafinance.entity.Compte;
import ega.api.egafinance.entity.Releve;
import ega.api.egafinance.entity.Transaction;
import ega.api.egafinance.exception.ResourceNotFoundException;
import ega.api.egafinance.mapper.TransactionMapper;
import ega.api.egafinance.repository.CompteRepository;
import ega.api.egafinance.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class TransactionService implements ITransactionService {

    private final CompteRepository compteRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionRepository transactionRepository;

    @Override
    public List<Transaction> getPagedTransactions(Pageable pageable) {
        Page<Transaction> transactionPage = transactionRepository.findAll(pageable);
        return transactionPage.getContent();
    }

    @Override
    @Transactional
    public Transaction versement(TransactionInput transactionInput) {
        BigDecimal montant = transactionInput.getMontant();
        String idCompteDest = transactionInput.getCompte_destination_Id();
        String idCompteSource = transactionInput.getCompte_source_Id();
        Transaction.TransactionType transactionType = transactionInput.getTransactionType();

        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à zéro !");
        }


        Compte compteDest = null;
        Compte compteSource = null;

        switch (transactionType) {
            case DEPOT:

                compteDest = compteRepository.findById(idCompteDest).orElseThrow(() -> new ResourceNotFoundException("Compte destination introuvable avec l'ID : " + idCompteDest));
                compteDest.setSolde(compteDest.getSolde().add(montant));
                compteRepository.save(compteDest);
                break;

            case RETRAIT:
                // Pour un retrait, seul le compte source est requis
                compteSource = compteRepository.findById(idCompteSource)
                        .orElseThrow(() -> new ResourceNotFoundException("Compte source introuvable avec l'ID : " + idCompteSource));

                if (compteSource.getSolde().compareTo(montant) < 0) {
                    throw new IllegalArgumentException("Fonds insuffisants sur le compte source !");
                }

                compteSource.setSolde(compteSource.getSolde().subtract(montant));
                compteRepository.save(compteSource);
                break;

            case VIREMENT:
                // Pour un virement, les comptes source et destination sont requis
                compteSource = compteRepository.findById(idCompteSource)
                        .orElseThrow(() -> new ResourceNotFoundException("Compte source introuvable avec l'ID : " + idCompteSource));
                compteDest = compteRepository.findById(idCompteDest)
                        .orElseThrow(() -> new ResourceNotFoundException("Compte destination introuvable avec l'ID : " + idCompteDest));

                if (compteSource.getSolde().compareTo(montant) < 0) {
                    throw new IllegalArgumentException("Fonds insuffisants sur le compte source !");
                }

                compteSource.setSolde(compteSource.getSolde().subtract(montant));
                compteDest.setSolde(compteDest.getSolde().add(montant));
                compteRepository.save(compteSource);
                compteRepository.save(compteDest);
                break;

            case PAIEMENT:
                // Pour un paiement, on traite cela comme un virement avec un compte destination "fournisseur"
                compteSource = compteRepository.findById(idCompteSource)
                        .orElseThrow(() -> new ResourceNotFoundException("Compte source introuvable avec l'ID : " + idCompteSource));
                compteDest = compteRepository.findById(idCompteDest)
                        .orElseThrow(() -> new ResourceNotFoundException("Compte destination introuvable avec l'ID : " + idCompteDest));

                if (compteSource.getSolde().compareTo(montant) < 0) {
                    throw new IllegalArgumentException("Fonds insuffisants pour le paiement !");
                }

                compteSource.setSolde(compteSource.getSolde().subtract(montant));
                compteDest.setSolde(compteDest.getSolde().add(montant));
                compteRepository.save(compteSource);
                compteRepository.save(compteDest);
                break;

            case REMBOURSEMENT:
                // Pour un remboursement, cela peut être traité comme un dépôt avec un historique de dette remboursée
                compteDest = compteRepository.findById(idCompteDest).orElseThrow(() -> new ResourceNotFoundException("Compte destination introuvable avec l'ID : " + idCompteDest));

                compteDest.setSolde(compteDest.getSolde().add(montant));
                compteRepository.save(compteDest);
                break;
            default:
                throw new IllegalArgumentException("Type de transaction non pris en charge !");
        }

        // Enregistrer la transaction
        Transaction transaction = transactionMapper.toTransaction(transactionInput);
        transaction.setMontant(montant);
        transaction.setCompteSource(compteSource);
        transaction.setCompteDestination(compteDest);
        transaction.setDateCreation(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }


    @Override
    public List<Transaction> getTransactionsByCompteAndPeriod(String compteId, LocalDateTime startDate, LocalDateTime endDate, String loggedUserRole, String loggedUserEmail) {

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La date de début doit être antérieure à la date de fin.");
        }
        // Récupérer le compte en question depuis la base
        Compte compte = compteRepository.findById(compteId).orElseThrow(() -> new ResourceNotFoundException("Compte introuvable avec l'ID : " + compteId));


        if (loggedUserRole.equals("ROLE_CLIENT")) {
            if (!compte.getClient().getEmail().equals(loggedUserEmail)) {
                throw new AccessDeniedException("Vous ne pouvez consulter les transactions que de vos propres comptes !");
            }
        }


        List<Transaction> sourceTransactions =
                transactionRepository.findAllByCompteSourceIdAndDateCreationBetween(compteId, startDate, endDate);
        List<Transaction> destinationTransactions =
                transactionRepository.findAllByCompteDestinationIdAndDateCreationBetween(compteId, startDate, endDate);

        sourceTransactions.addAll(destinationTransactions);
        return sourceTransactions;
    }

    public Releve getReleve(String compteId, LocalDateTime startDate, LocalDateTime endDate) {

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La date de début doit être antérieure à la date de fin !");
        }

        Compte compte = compteRepository.findById(compteId).orElseThrow(() -> new ResourceNotFoundException("Compte introuvable avec l'ID : " + compteId));

        BigDecimal soldeInitial = compte.getSolde();
        BigDecimal soldeFinal = soldeInitial;


        List<Transaction> allTransactions = transactionRepository.findAllByCompteSourceIdOrCompteDestinationId(compteId, compteId);

        // Liste pour stocker uniquement les transactions dans la période demandée
        List<Transaction> filteredTransactions = new ArrayList<>();

        for (Transaction transaction : allTransactions) {

            if (transaction.getDateCreation().isBefore(startDate)) {
                if (transaction.getCompteSource() != null && transaction.getCompteSource().getId().equals(compteId)) {
                    soldeInitial = soldeInitial.subtract(transaction.getMontant());
                } else if (transaction.getCompteDestination() != null && transaction.getCompteDestination().getId().equals(compteId)) {
                    soldeInitial = soldeInitial.add(transaction.getMontant());
                }
            }

            if (!transaction.getDateCreation().isBefore(startDate) && !transaction.getDateCreation().isAfter(endDate)) {
                filteredTransactions.add(transaction);

                if (transaction.getCompteSource() != null && transaction.getCompteSource().getId().equals(compteId)) {
                    soldeFinal = soldeFinal.subtract(transaction.getMontant());
                } else if (transaction.getCompteDestination() != null && transaction.getCompteDestination().getId().equals(compteId)) {
                    soldeFinal = soldeFinal.add(transaction.getMontant());
                }
            }
        }


        Releve releve = new Releve();
        releve.setCompte(compte);
        releve.setSoldeInitial(soldeInitial);
        releve.setSoldeFinal(soldeFinal);
        releve.setTransactions(filteredTransactions);

        return releve;
    }

}