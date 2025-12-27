package ega.api.egafinance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Compte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String numero;

    @Enumerated(EnumType.STRING)
    private TypeCompte typeCompte;

    private LocalDateTime dateCreation;

    private LocalDateTime dateUpdate;

    private BigDecimal solde;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id")
    private Client client;


    @PrePersist
    public void onCreate() {
        this.dateCreation = LocalDateTime.now();
        this.dateUpdate = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.dateUpdate = LocalDateTime.now();
    }
}