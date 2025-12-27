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

    @NotBlank(message = "Le numéro de compte est obligatoire")
    @Size(min = 10, max = 34, message = "Le numéro de compte doit contenir entre 10 et 34 caractères")
    @ValidIban(message = "Le numéro IBAN n'est pas valide")
    private String numero;

    @NotNull(message = "Le type de compte est obligatoire")
    private TypeCompte typeCompte;

    @NotNull(message = "Le solde initial est obligatoire")
    @DecimalMin(value = "0.0", message = "Le solde ne peut pas être négatif")
    private BigDecimal solde;

    @NotBlank(message = "L'identifiant du propriétaire est obligatoire")
    private String proprietaireId;
}