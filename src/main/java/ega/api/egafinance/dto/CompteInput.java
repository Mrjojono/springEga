package ega.api.egafinance.dto;

import ega.api.egafinance.entity.TypeCompte;
import ega.api.egafinance.validation.ValidIban;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompteInput {

    @NotNull(message = "Le type de compte est obligatoire")
    private TypeCompte typeCompte;

    private String libelle;

    @NotNull(message = "Le solde initial est obligatoire")
    @DecimalMin(value = "0.0", message = "Le solde ne peut pas être négatif")
    private BigDecimal solde;

    @NotBlank(message = "L'identifiant du propriétaire est obligatoire")
    private String proprietaireId;
}