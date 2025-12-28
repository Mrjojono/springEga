package ega.api.egafinance.controllers;

import ega.api.egafinance.dto.TransactionInput;
import ega.api.egafinance.entity.Compte;
import ega.api.egafinance.entity.Releve;
import ega.api.egafinance.entity.Transaction;
import ega.api.egafinance.exception.ResourceNotFoundException;
import ega.api.egafinance.service.CompteService;
import ega.api.egafinance.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final CompteService compteService;


    @QueryMapping
    @PreAuthorize("hasRole('AGENT_ADMIN') or hasRole('SUPER_ADMIN')")
    public List<Transaction> transactions(@Argument Integer page, @Argument Integer size) {
        int pageIndex = (page != null) ? page : 0;
        int pageSize = (size != null) ? size : 10;
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        return transactionService.getPagedTransactions(pageable);
    }



    @MutationMapping
    @PreAuthorize("hasRole('CLIENT') or hasRole('AGENT_ADMIN') or hasRole('SUPER_ADMIN')")
    public Transaction versement(@Argument("input") TransactionInput transactionInput) {
        Transaction transaction = transactionService.versement(transactionInput);
        if (transaction == null) {
            throw new IllegalStateException("Transaction non enregistrée !");
        }
        return transaction;
    }

    @QueryMapping
    @PreAuthorize("hasRole('CLIENT') or hasRole('AGENT_ADMIN') or hasRole('SUPER_ADMIN')")
    public List<Transaction> getTransactions(
            @Argument String compteId,
            @Argument String startDate,
            @Argument String endDate) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedUserEmail = authentication.getName();
        String loggedUserRole = authentication.getAuthorities().iterator().next().getAuthority();

        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);

        return transactionService.getTransactionsByCompteAndPeriod(compteId, start, end, loggedUserRole, loggedUserEmail);
    }


    @QueryMapping
    @PreAuthorize("hasRole('CLIENT') or hasRole('AGENT_ADMIN') or hasRole('SUPER_ADMIN')")
    public Releve getReleve(
            @Argument String compteId,
            @Argument String startDate,
            @Argument String endDate) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedUserEmail = authentication.getName();
        String loggedUserRole = authentication.getAuthorities().iterator().next().getAuthority();

        // Convertir les dates
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);


        Compte compte = compteService.showCompteById(compteId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable avec l'ID : " + compteId));
        if (loggedUserRole.equals("ROLE_CLIENT") && !compte.getClient().getEmail().equals(loggedUserEmail)) {
            throw new AccessDeniedException("Vous ne pouvez pas accéder au relevé de ce compte !");
        }


        return transactionService.getReleve(compteId, start, end);
    }
}