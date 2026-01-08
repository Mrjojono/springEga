package ega.api.egafinance.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@NoArgsConstructor
public class Client extends User {

    public enum Sexe {
        HOMME,
        FEMME,
        AUTRE
    }

    private LocalDate dateNaissance;

    @Enumerated(EnumType.STRING)
    private Sexe sexe;

    private String adresse;

    private String telephone;

    private String nom;

    private String prenom;

    private String nationalite;

    @Column(unique = true, nullable = false)
    private String identifiant;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Compte> comptes = new ArrayList<>();

    @PrePersist
    public void generateIdentifiant() {
        if (this.identifiant == null) {
            this.identifiant = "CLT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }
}