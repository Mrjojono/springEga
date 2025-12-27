package ega.api.egafinance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;



@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Client {

   public enum Sexe {
        HOMME,
        FEMME,
        AUTRE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String nom;

    private String prenom;

    private LocalDate dateNaissance;

    @Enumerated(EnumType.STRING)
    private Sexe sexe;

    private String adresse;

    private String email;

    private String telephone;

    private String nationalite;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Compte> comptes = new ArrayList<>();

}