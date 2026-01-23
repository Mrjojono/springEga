package ega.api.egafinance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReleveDTO {

    // En-tête du relevé
    private String numeroReleve;
    private String periode;
    private LocalDateTime dateGeneration;

    // Informations bancaires
    private String nomBanque = "EGA FINANCE";
    private String adresseBanque;
    private String telephoneBanque;

    // Informations du compte
    private String numeroCompte;
    private String typeCompte;
    private String libelleCompte;
    private String statutCompte;

    // Informations du client
    private String clientNom;
    private String clientPrenom;
    private String clientEmail;
    private String clientTelephone;
    private String clientAdresse;
    private String clientIdentifiant;

    // Période et soldes
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private BigDecimal soldeInitial;
    private BigDecimal soldeFinal;
    private BigDecimal variation;

    // Statistiques détaillées
    private StatistiquesReleve statistiques;

    // Transactions
    private List<TransactionReleveDTO> transactions;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatistiquesReleve {
        private int nombreTransactions;
        private BigDecimal totalDepots;
        private BigDecimal totalRetraits;
        private BigDecimal totalVirements;
        private BigDecimal totalPaiements;
        private int nombreDepots;
        private int nombreRetraits;
        private int nombreVirements;
        private int nombrePaiements;
        private BigDecimal montantMaxTransaction;
        private BigDecimal montantMinTransaction;
        private BigDecimal montantMoyenTransaction;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TransactionReleveDTO {
        private String id;
        private LocalDateTime date;
        private String type;
        private String description;
        private String reference;
        private BigDecimal montant;
        private String sens; // "CREDIT" ou "DEBIT"
        private BigDecimal soldeApres;
        private String compteContrePartie; // Numéro du compte source/destination
        private String nomContrePartie; // Nom du propriétaire du compte contrepartie
    }

    // Méthode utilitaire pour formater le relevé
    public String getEnteteFormate() {
        return String.format("""
            ═══════════════════════════════════════════════════════════════
                            RELEVÉ DE COMPTE
            ═══════════════════════════════════════════════════════════════
            
            Relevé N° : %s
            Période   : %s
            Généré le : %s
            
            ═══════════════════════════════════════════════════════════════
            """,
                numeroReleve,
                periode,
                dateGeneration != null ? dateGeneration.format(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")
                ) : ""
        );
    }

    public String getInfosClientFormatees() {
        return String.format("""
            CLIENT
            ───────────────────────────────────────────────────────────────
            Nom           : %s %s
            Identifiant   : %s
            Email         : %s
            Téléphone     : %s
            Adresse       : %s
            
            """,
                clientNom != null ? clientNom : "",
                clientPrenom != null ? clientPrenom : "",
                clientIdentifiant != null ? clientIdentifiant : "",
                clientEmail != null ? clientEmail : "",
                clientTelephone != null ? clientTelephone : "",
                clientAdresse != null ? clientAdresse : ""
        );
    }

    public String getInfosCompteFormatees() {
        return String.format("""
            COMPTE
            ───────────────────────────────────────────────────────────────
            Numéro        : %s
            Type          : %s
            Libellé       : %s
            Statut        : %s
            
            """,
                numeroCompte != null ? numeroCompte : "",
                typeCompte != null ? typeCompte : "",
                libelleCompte != null ? libelleCompte : "",
                statutCompte != null ? statutCompte : ""
        );
    }

    public String getSoldesFormats() {
        return String.format("""
            SOLDES
            ───────────────────────────────────────────────────────────────
            Solde initial : %,.2f FCFA
            Solde final   : %,.2f FCFA
            Variation     : %,.2f FCFA (%s)
            
            """,
                soldeInitial != null ? soldeInitial : BigDecimal.ZERO,
                soldeFinal != null ? soldeFinal : BigDecimal.ZERO,
                variation != null ? variation : BigDecimal.ZERO,
                variation != null && variation.compareTo(BigDecimal.ZERO) >= 0 ? "↑" : "↓"
        );
    }
}