package ega.api.egafinance.dto;

import ega.api.egafinance.entity.Client;
import ega.api.egafinance.entity.User;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MeResponse {

    // User
    private String id;
    private String nom;
    private String prenom;
    private String email;
    private User.Role role;

    // Client
    private LocalDate dateNaissance;
    private Client.Sexe sexe;
    private String adresse;
    private String telephone;
    private String nationalite;
    private String identifiant;

    public static MeResponse fromClient(Client client) {
        MeResponse dto = new MeResponse();
        dto.setId(client.getId());
        dto.setNom(client.getNom());
        dto.setPrenom(client.getPrenom());
        dto.setEmail(client.getEmail());
        dto.setRole(client.getRole());

        dto.setDateNaissance(client.getDateNaissance());
        dto.setSexe(client.getSexe());
        dto.setAdresse(client.getAdresse());
        dto.setTelephone(client.getTelephone());
        dto.setNationalite(client.getNationalite());
        dto.setIdentifiant(client.getIdentifiant());

        return dto;
    }
}
