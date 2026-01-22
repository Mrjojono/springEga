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
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
            case DEPOT, REMBOURSEMENT:
                compteDest = compteRepository.findById(idCompteDest)
                        .orElseThrow(() -> new ResourceNotFoundException("Compte destination introuvable avec l'ID : " + idCompteDest));
                compteDest.setSolde(compteDest.getSolde().add(montant));
                compteRepository.save(compteDest);
                break;

            case RETRAIT:
                compteSource = compteRepository.findById(idCompteSource)
                        .orElseThrow(() -> new ResourceNotFoundException("Compte source introuvable avec l'ID : " + idCompteSource));

                if (compteSource.getSolde().compareTo(montant) < 0) {
                    throw new IllegalArgumentException("Fonds insuffisants sur le compte source !");
                }

                compteSource.setSolde(compteSource.getSolde().subtract(montant));
                compteRepository.save(compteSource);
                break;

            case VIREMENT:
            case PAIEMENT:
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

        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable avec l'ID : " + compteId));

        if (loggedUserRole != null && loggedUserRole.equalsIgnoreCase("ROLE_CLIENT")) {
            if (compte.getClient() == null || compte.getClient().getEmail() == null ||
                    !compte.getClient().getEmail().equalsIgnoreCase(loggedUserEmail)) {
                throw new AccessDeniedException("Vous ne pouvez consulter les transactions que de vos propres comptes !");
            }
        }

        // Récupérer toutes les transactions liées au compte (source OU destination)
        List<Transaction> sourceTransactions =
                transactionRepository.findAllByCompteSourceIdAndDateCreationBetween(compteId, startDate, endDate);
        List<Transaction> destinationTransactions =
                transactionRepository.findAllByCompteDestinationIdAndDateCreationBetween(compteId, startDate, endDate);

        // Fusionner et trier par date de création asc
        List<Transaction> combined = new ArrayList<>();
        if (sourceTransactions != null) combined.addAll(sourceTransactions);
        if (destinationTransactions != null) combined.addAll(destinationTransactions);

        combined.sort(Comparator.comparing(Transaction::getDateCreation));

        return combined;
    }


    public Releve getReleve(String compteId, LocalDateTime startDate, LocalDateTime endDate) {

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La date de début doit être antérieure à la date de fin !");
        }

        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable avec l'ID : " + compteId));


        List<Transaction> allTransactions = transactionRepository.findAllByCompteSourceIdOrCompteDestinationId(compteId, compteId);


        BigDecimal deltaAfterOrEqualStart = BigDecimal.ZERO;


        List<Transaction> filteredTransactions = new ArrayList<>();

        for (Transaction tx : allTransactions) {
            LocalDateTime txDate = tx.getDateCreation();
            if (txDate == null) continue;

            // effet de la transaction sur le compte (positive si le compte est destination, négative si source)
            BigDecimal effect = BigDecimal.ZERO;
            if (tx.getCompteSource() != null && compteId.equals(tx.getCompteSource().getId())) {
                effect = tx.getMontant().negate();
            } else if (tx.getCompteDestination() != null && compteId.equals(tx.getCompteDestination().getId())) {
                effect = tx.getMontant();
            }

            // si transaction >= startDate, elle participe au deltaAfterOrEqualStart (à retrancher pour retrouver le solde au start)
            if (!txDate.isBefore(startDate)) {
                deltaAfterOrEqualStart = deltaAfterOrEqualStart.add(effect);
            }

            // si transaction est dans la période [startDate, endDate], on l'ajoute au relevé
            if ((!txDate.isBefore(startDate)) && (!txDate.isAfter(endDate))) {
                filteredTransactions.add(tx);
            }
        }

        // solde courant = compte.getSolde() (final actuel)
        BigDecimal soldeCourant = compte.getSolde() != null ? compte.getSolde() : BigDecimal.ZERO;

        // soldeInitial = soldeCourant - deltaAfterOrEqualStart
        BigDecimal soldeInitial = soldeCourant.subtract(deltaAfterOrEqualStart);

        // calculer soldeFinal en appliquant les effets des transactions filtrées
        BigDecimal soldeFinal = soldeInitial;
        for (Transaction tx : filteredTransactions) {
            if (tx.getCompteSource() != null && compteId.equals(tx.getCompteSource().getId())) {
                soldeFinal = soldeFinal.subtract(tx.getMontant());
            } else if (tx.getCompteDestination() != null && compteId.equals(tx.getCompteDestination().getId())) {
                soldeFinal = soldeFinal.add(tx.getMontant());
            }
        }

        // Trier les transactions du relevé
        filteredTransactions.sort(Comparator.comparing(Transaction::getDateCreation));

        Releve releve = new Releve();
        releve.setCompte(compte);
        releve.setSoldeInitial(soldeInitial);
        releve.setSoldeFinal(soldeFinal);
        releve.setTransactions(filteredTransactions);

        return releve;
    }
}