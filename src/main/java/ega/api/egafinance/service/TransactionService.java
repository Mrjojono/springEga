package ega.api.egafinance.service;

import ega.api.egafinance.dto.TransactionInput;
import ega.api.egafinance.entity.Compte;
import ega.api.egafinance.entity.Releve;
import ega.api.egafinance.entity.Transaction;
import ega.api.egafinance.entity.TransactionType;
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

import static ega.api.egafinance.entity.TransactionType.DEPOT;
import static ega.api.egafinance.entity.TransactionType.REMBOURSEMENT;

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

        // transactionInput.getTransactionType() doit renvoyer une chaîne (ex: "DEPOT")
        String txRaw = transactionInput.getTransactionType();
        if (txRaw == null || txRaw.trim().isEmpty()) {
            throw new IllegalArgumentException("Le type de transaction est requis");
        }

        // Parse safe vers l'enum
        final TransactionType transactionType;
        try {
            transactionType = TransactionType.valueOf(txRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Type de transaction invalide : " + txRaw);
        }

        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à zéro !");
        }

        Compte compteDest = null;
        Compte compteSource = null;

        switch (transactionType) {
            case DEPOT:
            case REMBOURSEMENT:
                if (idCompteDest == null || idCompteDest.trim().isEmpty()) {
                    throw new IllegalArgumentException("L'ID du compte destination est requis pour un dépôt/ remboursement");
                }
                compteDest = compteRepository.findById(idCompteDest)
                        .orElseThrow(() -> new ResourceNotFoundException("Compte destination introuvable avec l'ID : " + idCompteDest));
                compteDest.setSolde(compteDest.getSolde().add(montant));
                compteRepository.save(compteDest);
                break;

            case RETRAIT:
                if (idCompteSource == null || idCompteSource.trim().isEmpty()) {
                    throw new IllegalArgumentException("L'ID du compte source est requis pour un retrait");
                }
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
                if (idCompteSource == null || idCompteSource.trim().isEmpty() || idCompteDest == null || idCompteDest.trim().isEmpty()) {
                    throw new IllegalArgumentException("Les IDs des comptes source et destination sont requis pour un virement/paiement");
                }
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

        // S'assurer que l'entité a bien l'enum (si le mapper n'a reçu qu'une String)
        transaction.setTransactionType(transactionType);

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

        // Récupérer toutes les transactions liées au compte
        List<Transaction> allTransactions = transactionRepository
                .findAllByCompteSourceIdOrCompteDestinationId(compteId, compteId);

        BigDecimal deltaAfterOrEqualStart = BigDecimal.ZERO;
        List<Transaction> filteredTransactions = new ArrayList<>();

        // Statistiques
        BigDecimal totalDepots = BigDecimal.ZERO;
        BigDecimal totalRetraits = BigDecimal.ZERO;
        BigDecimal totalVirements = BigDecimal.ZERO;

        for (Transaction tx : allTransactions) {
            LocalDateTime txDate = tx.getDateCreation();
            if (txDate == null) continue;

            // Effet de la transaction sur le compte
            BigDecimal effect = BigDecimal.ZERO;
            boolean isSource = tx.getCompteSource() != null && compteId.equals(tx.getCompteSource().getId());
            boolean isDestination = tx.getCompteDestination() != null && compteId.equals(tx.getCompteDestination().getId());

            if (isSource) {
                effect = tx.getMontant().negate();
            } else if (isDestination) {
                effect = tx.getMontant();
            }

            // Transactions >= startDate
            if (!txDate.isBefore(startDate)) {
                deltaAfterOrEqualStart = deltaAfterOrEqualStart.add(effect);
            }

            // Transactions dans la période [startDate, endDate]
            if ((!txDate.isBefore(startDate)) && (!txDate.isAfter(endDate))) {
                filteredTransactions.add(tx);

                // Calculer les statistiques selon le type
                TransactionType type = tx.getTransactionType();
                if (type != null) {
                    switch (type) {
                        case DEPOT:
                        case REMBOURSEMENT:
                            if (isDestination) {
                                totalDepots = totalDepots.add(tx.getMontant());
                            }
                            break;
                        case RETRAIT:
                            if (isSource) {
                                totalRetraits = totalRetraits.add(tx.getMontant());
                            }
                            break;
                        case VIREMENT:
                        case PAIEMENT:
                            if (isSource) {
                                totalVirements = totalVirements.add(tx.getMontant());
                            }
                            break;
                    }
                }
            }
        }

        // Calcul des soldes
        BigDecimal soldeCourant = compte.getSolde() != null ? compte.getSolde() : BigDecimal.ZERO;
        BigDecimal soldeInitial = soldeCourant.subtract(deltaAfterOrEqualStart);

        BigDecimal soldeFinal = soldeInitial;
        for (Transaction tx : filteredTransactions) {
            if (tx.getCompteSource() != null && compteId.equals(tx.getCompteSource().getId())) {
                soldeFinal = soldeFinal.subtract(tx.getMontant());
            } else if (tx.getCompteDestination() != null && compteId.equals(tx.getCompteDestination().getId())) {
                soldeFinal = soldeFinal.add(tx.getMontant());
            }
        }

        // Trier les transactions
        filteredTransactions.sort(Comparator.comparing(Transaction::getDateCreation));

        // Construire le relevé enrichi
        Releve releve = new Releve();
        releve.setCompte(compte);

        // Informations du client
        if (compte.getClient() != null) {
            releve.setClientNom(compte.getClient().getNom());
            releve.setClientPrenom(compte.getClient().getPrenom());
            releve.setClientEmail(compte.getClient().getEmail());
            releve.setClientTelephone(compte.getClient().getTelephone());
            releve.setClientAdresse(compte.getClient().getAdresse());
            releve.setClientIdentifiant(compte.getClient().getIdentifiant());
        }

        // Dates
        releve.setDateDebut(startDate);
        releve.setDateFin(endDate);
        releve.setDateGeneration(LocalDateTime.now());

        // Soldes
        releve.setSoldeInitial(soldeInitial);
        releve.setSoldeFinal(soldeFinal);

        // Statistiques
        releve.setTotalDepots(totalDepots);
        releve.setTotalRetraits(totalRetraits);
        releve.setTotalVirements(totalVirements);
        releve.setNombreTransactions(filteredTransactions.size());

        // Transactions
        releve.setTransactions(filteredTransactions);

        // Générer un numéro de relevé unique
        String numeroReleve = String.format("REL-%s-%s",
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
        releve.setNumeroReleve(numeroReleve);

        // Format de période lisible
        String periode = String.format("Du %s au %s",
                startDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                endDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );
        releve.setPeriode(periode);

        return releve;
    }





}