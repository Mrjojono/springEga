package ega.api.egafinance.dto;

import ega.api.egafinance.entity.TypeCompte;
import ega.api.egafinance.validation.ValidIban;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompteUpdateInput {

    @ValidIban(message = "Le numéro IBAN n'est pas valide")
    private String numero;

    private BigDecimal solde;

    private TypeCompte typeCompte;

    private String proprietaireId;
}
