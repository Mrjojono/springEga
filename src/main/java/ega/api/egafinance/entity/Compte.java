package ega.api.egafinance.entity;

import ega.api.egafinance.validation.ValidIban;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Le numéro de compte est obligatoire")
    @Size(min = 10, max = 34, message = "Le numéro de compte doit contenir entre 10 et 34 caractères")
    @ValidIban(message = "Le numéro IBAN n'est pas valide")
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