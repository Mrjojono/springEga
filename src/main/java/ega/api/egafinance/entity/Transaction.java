package ega.api.egafinance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "\"transaction\"")
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

    private BigDecimal montant;

    private LocalDateTime dateCreation;

    private LocalDateTime dateUpdate;

    private  TransactionType transactionType;

    @ManyToOne
    @JoinColumn(name = "compte_source_id")
    private  Compte compteSource;

    @ManyToOne
    @JoinColumn(name = "compte_destination_id")
    private  Compte compteDestination;

    @PrePersist
    public void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.dateUpdate = LocalDateTime.now();
    }

}
