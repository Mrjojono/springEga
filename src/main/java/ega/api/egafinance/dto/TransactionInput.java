package ega.api.egafinance.dto;

import ega.api.egafinance.entity.Transaction;
import ega.api.egafinance.entity.TypeCompte;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionInput {

    private BigDecimal montant;

    private String compte_source_Id;

    private Transaction.TransactionType transactionType;

    private String compte_destination_Id;
}
