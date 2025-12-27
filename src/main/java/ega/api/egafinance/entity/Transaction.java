package ega.api.egafinance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.DecimalFormat;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Transaction {


    public enum TransactionType {
        DEPOT,
        RETRAIT,
        VIREMENT,
        PAIEMENT,
        FRAIS,
        INTERET,
        REMBOURSEMENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private DecimalFormat montant;

    private LocalDateTime date;

    @ManyToOne
    @JoinColumn(name = "compte_source_id")
    private  Compte compteSource;

    @ManyToOne
    @JoinColumn(name = "compte_destination_id")
    private  Compte compteDestination;

}
