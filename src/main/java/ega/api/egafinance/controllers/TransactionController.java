package ega.api.egafinance.controllers;

import ega.api.egafinance.dto.TransactionInput;
import ega.api.egafinance.entity.Transaction;
import ega.api.egafinance.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @QueryMapping
    public List<Transaction> transactions(@Argument Integer page, @Argument Integer size) {
        int pageIndex = (page != null) ? page : 0;
        int pageSize = (size != null) ? size : 10;
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        return transactionService.getPagedTransactions(pageable);
    }


    @MutationMapping
    public Transaction versement(@Argument("input") TransactionInput transactionInput) {
        Transaction transaction = transactionService.versement(transactionInput);
        if (transaction == null) {
            throw new IllegalStateException("Transaction non enregistrée !");
        }
        return transaction;
    }
}