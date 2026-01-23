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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final CompteService compteService;

    @QueryMapping
    @PreAuthorize("hasRole('AGENT_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('CLIENT') ")
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

        if (compteId == null || compteId.isBlank()) {
            throw new IllegalArgumentException("Le paramètre compteId est requis.");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Les paramètres startDate et endDate sont requis.");
        }

        LocalDateTime start;
        LocalDateTime end;
        try {
            start = LocalDateTime.parse(startDate);
            end = LocalDateTime.parse(endDate);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Format de date invalide. Utilisez un format ISO-8601 (ex: 2024-03-01T00:00:00).", ex);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Utilisateur non authentifié.");
        }

        // Récupérer l'email principal (getName retourne ici l'email)
        String loggedUserEmail = authentication.getName();

        // Récupérer le rôle de l'utilisateur de façon sûre (on prend le premier rôle si existe)
        String loggedUserRole = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");

        return transactionService.getTransactionsByCompteAndPeriod(compteId, start, end, loggedUserRole, loggedUserEmail);
    }

    @QueryMapping
    @PreAuthorize("hasRole('CLIENT') or hasRole('AGENT_ADMIN') or hasRole('SUPER_ADMIN')")
    public Releve getReleves(
            @Argument String compteId,
            @Argument String startDate,
            @Argument String endDate) {

        if (compteId == null || compteId.isBlank()) {
            throw new IllegalArgumentException("Le paramètre compteId est requis.");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Les paramètres startDate et endDate sont requis.");
        }

        LocalDateTime start;
        LocalDateTime end;
        try {
            start = LocalDateTime.parse(startDate);
            end = LocalDateTime.parse(endDate);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Format de date invalide. Utilisez un format ISO-8601 (ex: 2024-03-01T00:00:00).", ex);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Utilisateur non authentifié.");
        }

        String loggedUserEmail = authentication.getName();
        String loggedUserRole = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");

        Compte compte = compteService.showCompteById(compteId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable avec l'ID : " + compteId));

        if ("ROLE_CLIENT".equalsIgnoreCase(loggedUserRole) && (compte.getClient() == null || compte.getClient().getEmail() == null
                || !compte.getClient().getEmail().equalsIgnoreCase(loggedUserEmail))) {
            throw new AccessDeniedException("Vous ne pouvez pas accéder au relevé de ce compte !");
        }

        return transactionService.getReleve(compteId, start, end);
    }
}