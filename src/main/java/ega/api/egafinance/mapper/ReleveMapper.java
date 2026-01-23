package ega.api.egafinance.mapper;

import ega.api.egafinance.dto.ReleveDTO;
import ega.api.egafinance.entity.Releve;
import ega.api.egafinance.entity.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReleveMapper {

    public ReleveDTO toDTO(Releve releve) {
        if (releve == null) {
            return null;
        }

        ReleveDTO dto = new ReleveDTO();

        // En-tête
        dto.setNumeroReleve(releve.getNumeroReleve());
        dto.setPeriode(releve.getPeriode());
        dto.setDateGeneration(releve.getDateGeneration());

        // Informations du compte
        if (releve.getCompte() != null) {
            dto.setNumeroCompte(releve.getCompte().getNumero());
            dto.setTypeCompte(releve.getCompte().getTypeCompte() != null ?
                    releve.getCompte().getTypeCompte().toString() : null);
            dto.setLibelleCompte(releve.getCompte().getLibelle());
            dto.setStatutCompte(releve.getCompte().getStatutCompte() != null ?
                    releve.getCompte().getStatutCompte().toString() : null);
        }

        // Informations du client
        dto.setClientNom(releve.getClientNom());
        dto.setClientPrenom(releve.getClientPrenom());
        dto.setClientEmail(releve.getClientEmail());
        dto.setClientTelephone(releve.getClientTelephone());
        dto.setClientAdresse(releve.getClientAdresse());
        dto.setClientIdentifiant(releve.getClientIdentifiant());

        // Période et soldes
        dto.setDateDebut(releve.getDateDebut());
        dto.setDateFin(releve.getDateFin());
        dto.setSoldeInitial(releve.getSoldeInitial());
        dto.setSoldeFinal(releve.getSoldeFinal());

        BigDecimal variation = releve.getSoldeFinal().subtract(releve.getSoldeInitial());
        dto.setVariation(variation);

        // Statistiques
        dto.setStatistiques(buildStatistiques(releve));

        // Transactions
        dto.setTransactions(buildTransactionsDTO(releve));

        return dto;
    }

    private ReleveDTO.StatistiquesReleve buildStatistiques(Releve releve) {
        ReleveDTO.StatistiquesReleve stats = new ReleveDTO.StatistiquesReleve();

        stats.setNombreTransactions(releve.getNombreTransactions());
        stats.setTotalDepots(releve.getTotalDepots());
        stats.setTotalRetraits(releve.getTotalRetraits());
        stats.setTotalVirements(releve.getTotalVirements());

        // Calculer des statistiques supplémentaires
        List<Transaction> transactions = releve.getTransactions();
        if (transactions != null && !transactions.isEmpty()) {
            int nbDepots = 0, nbRetraits = 0, nbVirements = 0, nbPaiements = 0;
            BigDecimal totalPaiements = BigDecimal.ZERO;
            BigDecimal max = transactions.get(0).getMontant();
            BigDecimal min = transactions.get(0).getMontant();
            BigDecimal sum = BigDecimal.ZERO;

            for (Transaction tx : transactions) {
                BigDecimal montant = tx.getMontant();
                sum = sum.add(montant);

                if (montant.compareTo(max) > 0) max = montant;
                if (montant.compareTo(min) < 0) min = montant;

                if (tx.getTransactionType() != null) {
                    switch (tx.getTransactionType()) {
                        case DEPOT:
                        case REMBOURSEMENT:
                            nbDepots++;
                            break;
                        case RETRAIT:
                            nbRetraits++;
                            break;
                        case VIREMENT:
                            nbVirements++;
                            break;
                        case PAIEMENT:
                            nbPaiements++;
                            totalPaiements = totalPaiements.add(montant);
                            break;
                    }
                }
            }

            stats.setNombreDepots(nbDepots);
            stats.setNombreRetraits(nbRetraits);
            stats.setNombreVirements(nbVirements);
            stats.setNombrePaiements(nbPaiements);
            stats.setTotalPaiements(totalPaiements);
            stats.setMontantMaxTransaction(max);
            stats.setMontantMinTransaction(min);
            stats.setMontantMoyenTransaction(
                    sum.divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP)
            );
        }

        return stats;
    }

    private List<ReleveDTO.TransactionReleveDTO> buildTransactionsDTO(Releve releve) {
        if (releve.getTransactions() == null || releve.getCompte() == null) {
            return new ArrayList<>();
        }

        String compteId = releve.getCompte().getId();
        BigDecimal soldeActuel = releve.getSoldeInitial();
        List<ReleveDTO.TransactionReleveDTO> result = new ArrayList<>();

        for (Transaction tx : releve.getTransactions()) {
            ReleveDTO.TransactionReleveDTO txDTO = new ReleveDTO.TransactionReleveDTO();

            txDTO.setId(tx.getId());
            txDTO.setDate(tx.getDateCreation());
            txDTO.setType(tx.getTransactionType() != null ?
                    tx.getTransactionType().toString() : "");
            txDTO.setMontant(tx.getMontant());

            // Déterminer le sens et le compte contrepartie
            boolean isSource = tx.getCompteSource() != null &&
                    compteId.equals(tx.getCompteSource().getId());
            boolean isDestination = tx.getCompteDestination() != null &&
                    compteId.equals(tx.getCompteDestination().getId());

            if (isSource) {
                txDTO.setSens("DEBIT");
                soldeActuel = soldeActuel.subtract(tx.getMontant());

                if (tx.getCompteDestination() != null) {
                    txDTO.setCompteContrePartie(tx.getCompteDestination().getNumero());
                    if (tx.getCompteDestination().getClient() != null) {
                        txDTO.setNomContrePartie(
                                tx.getCompteDestination().getClient().getNom() + " " +
                                        tx.getCompteDestination().getClient().getPrenom()
                        );
                    }
                }
            } else if (isDestination) {
                txDTO.setSens("CREDIT");
                soldeActuel = soldeActuel.add(tx.getMontant());

                if (tx.getCompteSource() != null) {
                    txDTO.setCompteContrePartie(tx.getCompteSource().getNumero());
                    if (tx.getCompteSource().getClient() != null) {
                        txDTO.setNomContrePartie(
                                tx.getCompteSource().getClient().getNom() + " " +
                                        tx.getCompteSource().getClient().getPrenom()
                        );
                    }
                }
            }

            txDTO.setSoldeApres(soldeActuel);

            // Générer une description
            String description = genererDescription(tx, isSource, isDestination);
            txDTO.setDescription(description);

            // Référence unique
            txDTO.setReference("TRX-" + tx.getId());

            result.add(txDTO);
        }

        return result;
    }

    private String genererDescription(Transaction tx, boolean isSource, boolean isDestination) {
        if (tx.getTransactionType() == null) {
            return "Transaction";
        }

        switch (tx.getTransactionType()) {
            case DEPOT:
                return "Dépôt d'espèces";
            case RETRAIT:
                return "Retrait d'espèces";
            case VIREMENT:
                if (isSource) {
                    return "Virement émis vers " +
                            (tx.getCompteDestination() != null ?
                                    tx.getCompteDestination().getNumero() : "");
                } else {
                    return "Virement reçu de " +
                            (tx.getCompteSource() != null ?
                                    tx.getCompteSource().getNumero() : "");
                }
            case PAIEMENT:
                return isSource ? "Paiement effectué" : "Paiement reçu";
            case REMBOURSEMENT:
                return "Remboursement";
            default:
                return "Transaction";
        }
    }
}