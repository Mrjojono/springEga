package ega.api.egafinance.service;

import ega.api.egafinance.dto.TransactionInput;
import ega.api.egafinance.entity.Transaction;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ITransactionService {


    //recuperer  les transactions en base de données
    public List<Transaction> getPagedTransactions(Pageable pageable);

    //function pour effectuer un versement sur un compte
    public Transaction versement(TransactionInput transactionInput);
}
