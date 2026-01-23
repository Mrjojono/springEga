package ega.api.egafinance.controllers;

import ega.api.egafinance.dto.ReleveDTO;
import ega.api.egafinance.entity.Releve;
import ega.api.egafinance.mapper.ReleveMapper;
import ega.api.egafinance.service.RelevePdfService;
import ega.api.egafinance.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Base64;

@Controller
@RequiredArgsConstructor
public class ReleveGraphqlController {

    private final TransactionService transactionService;
    private final RelevePdfService relevePdfService;
    private final ReleveMapper releveMapper;

    /**
     * GraphQL query — retourne le relevé JSON (DTO)
     */
    @QueryMapping(name = "getReleve")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT_ADMIN', 'SUPER_ADMIN')")
    public ReleveDTO getReleve(
            @Argument String compteId,
            @Argument String startDate,
            @Argument String endDate,
            Authentication authentication
    ) {
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
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Format de date invalide. Utilisez un format ISO-8601 (ex: 2024-03-01T00:00:00).", ex);
        }

        // sécurité : authentication peut être null en config non sécurisée
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Utilisateur non authentifié.");
        }

        // Récupérer le relevé côté service
        Releve releve = transactionService.getReleve(compteId, start, end);

        // Mapper vers DTO
        return releveMapper.toDTO(releve);
    }

    /**
     * GraphQL query — retourne le PDF du relevé encodé en Base64 (pratique pour le front)
     */
    @QueryMapping(name = "getRelevePdfBase64")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT_ADMIN', 'SUPER_ADMIN')")
    public String getRelevePdfBase64(
            @Argument String compteId,
            @Argument String startDate,
            @Argument String endDate,
            Authentication authentication
    ) {
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
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Format de date invalide. Utilisez un format ISO-8601 (ex: 2024-03-01T00:00:00).", ex);
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Utilisateur non authentifié.");
        }

        try {
            Releve releve = transactionService.getReleve(compteId, start, end);
            ReleveDTO releveDTO = releveMapper.toDTO(releve);
            byte[] pdfBytes = relevePdfService.generateRelevePdf(releveDTO);
            return Base64.getEncoder().encodeToString(pdfBytes);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    /**
     * GraphQL query — preview (renvoie aussi base64)
     */
    @QueryMapping(name = "previewRelevePdfBase64")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT_ADMIN', 'SUPER_ADMIN')")
    public String previewRelevePdfBase64(
            @Argument String compteId,
            @Argument String startDate,
            @Argument String endDate,
            Authentication authentication
    ) {
        // Réutilise la même logique que getRelevePdfBase64
        return getRelevePdfBase64(compteId, startDate, endDate, authentication);
    }

    /**
     * GraphQL mutation — send releve by email (TODO: implémenter l'envoi réel)
     */
    @MutationMapping(name = "sendReleveByEmail")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT_ADMIN', 'SUPER_ADMIN')")
    public String sendReleveByEmail(
            @Argument String compteId,
            @Argument String startDate,
            @Argument String endDate,
            @Argument String customEmail,
            Authentication authentication
    ) {
        // Vérifs de base
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
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Format de date invalide. Utilisez un format ISO-8601 (ex: 2024-03-01T00:00:00).", ex);
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Utilisateur non authentifié.");
        }

        // TODO: implémenter l'envoi avec JavaMailSender ou un service mail
        // Pour l'instant on génère le PDF (vérif) et renvoie un message.
        try {
            Releve releve = transactionService.getReleve(compteId, start, end);
            ReleveDTO releveDTO = releveMapper.toDTO(releve);
            byte[] pdfBytes = relevePdfService.generateRelevePdf(releveDTO);
            // Ici tu enverrais pdfBytes + destinatataire (customEmail / utilisateur connecté)
            return "Fonctionnalité d'envoi par email non implémentée. PDF généré (" + pdfBytes.length + " bytes).";
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }
}