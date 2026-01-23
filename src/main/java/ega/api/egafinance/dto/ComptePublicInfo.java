package ega.api.egafinance.dto;

import ega.api.egafinance.entity.TypeCompte;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComptePublicInfo {
    private String id;
    private String numero;
    private TypeCompte typeCompte;
    private String libelle;
    private String proprietaireNom;
    private String proprietaireEmail;

    // Getters et setters...
}