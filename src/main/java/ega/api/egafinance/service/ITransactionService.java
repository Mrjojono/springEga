package ega.api.egafinance.service;

import ega.api.egafinance.dto.TransactionInput;
import ega.api.egafinance.entity.Transaction;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ITransactionService {


    //recuperer  les transactions en base de données
    public List<Transaction> getPagedTransactions(Pageable pageable);


    //récupère les transactions sur une période données
    public List<Transaction> getTransactionsByCompteAndPeriod(
            String compteId, LocalDateTime startDate, LocalDateTime endDate, String loggedUserRole, String loggedUserEmail);

    //function pour effectuer un versement sur un compte
    public Transaction versement(TransactionInput transactionInput);
}
