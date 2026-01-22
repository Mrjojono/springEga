package ega.api.egafinance.dto;

import ega.api.egafinance.entity.User.Role;
import ega.api.egafinance.entity.Client.Sexe;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInput {

    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String password;
    private Role role;
    private Sexe sexe;
    private String adresse;
    private String nationalite;

}