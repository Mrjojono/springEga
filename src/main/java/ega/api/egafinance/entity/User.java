package ega.api.egafinance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "app_user")
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String nom;
    private String prenom;

    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.CLIENT;

    // Enumération des rôles possibles
    public enum Role {
        CLIENT,
        AGENT_ADMIN,
        SUPER_ADMIN,
        USER,
        ADMIN
    }

    @PrePersist
    public void prePersist() {
        if (this.role == null) {
            this.role = Role.CLIENT;
        }
    }



}