package ega.api.egafinance.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Releve {
    // Informations du compte
    private Compte compte;

    // Informations du client propriétaire
    private String clientNom;
    private String clientPrenom;
    private String clientEmail;
    private String clientTelephone;
    private String clientAdresse;
    private String clientIdentifiant;

    // Informations du relevé
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private LocalDateTime dateGeneration;

    // Soldes
    private BigDecimal soldeInitial;
    private BigDecimal soldeFinal;

    // Statistiques de la période
    private BigDecimal totalDepots;
    private BigDecimal totalRetraits;
    private BigDecimal totalVirements;
    private int nombreTransactions;

    // Liste des transactions
    private List<Transaction> transactions;

    // Informations additionnelles
    private String numeroReleve; // Format: REL-YYYYMMDD-XXXXX
    private String periode; // Format lisible: "Du 01/01/2026 au 31/01/2026"
}