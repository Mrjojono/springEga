package ega.api.egafinance.dto;

import ega.api.egafinance.entity.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
public class ClientInput {
    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String prenom;

    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDate dateNaissance;

    private Client.Sexe sexe;

    @Size(max = 200, message = "L'adresse ne peut pas dépasser 200 caractères")
    private String adresse;

    @Email(message = "L'email n'est pas valide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{8,15}$", message = "Le numéro de téléphone n'est pas valide")
    private String telephone;

    @Size(max = 50, message = "La nationalité ne peut pas dépasser 50 caractères")
    private String nationalite;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private User.Role role;
}