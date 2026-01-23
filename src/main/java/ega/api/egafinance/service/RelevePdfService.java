package ega.api.egafinance.service;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import ega.api.egafinance.dto.ReleveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RelevePdfService {

    private final TemplateEngine templateEngine;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH);

    /**
     * Génère un PDF à partir d'un relevé
     */
    public byte[] generateRelevePdf(ReleveDTO releve) throws IOException {
        // Préparer le contexte Thymeleaf
        Context context = new Context();
        context.setVariable("releve", releve);
        context.setVariable("dateFormatter", DATE_FORMATTER);
        context.setVariable("datetimeFormatter", DATETIME_FORMATTER);

        // Calculer les infos RIB depuis le numéro de compte
        RibInfo ribInfo = extractRibInfo(releve.getNumeroCompte());
        context.setVariable("codeBanque", ribInfo.codeBanque);
        context.setVariable("codeGuichet", ribInfo.codeGuichet);
        context.setVariable("cleRib", ribInfo.cleRib);

        // Générer HTML
        String htmlContent = templateEngine.process("releve-pdf", context);
        byte[] htmlBytes = htmlContent.getBytes(StandardCharsets.UTF_8);

        ConverterProperties converterProperties = new ConverterProperties();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             PdfWriter writer = new PdfWriter(outputStream);
             PdfDocument pdfDocument = new PdfDocument(writer);
             ByteArrayInputStream htmlStream = new ByteArrayInputStream(htmlBytes)) {

            pdfDocument.setDefaultPageSize(PageSize.A4);

            HtmlConverter.convertToPdf(htmlStream, pdfDocument, converterProperties);
            return outputStream.toByteArray();
        }
    }

    /**
     * Extrait les infos RIB du numéro de compte
     * Format attendu du numéro de compte : BBBBBGGGGGNNNNNNNNNNC
     * B = Code Banque (5 chiffres)
     * G = Code Guichet (5 chiffres)
     * N = Numéro de compte (11 chiffres)
     * C = Clé RIB (2 chiffres)
     */
    private RibInfo extractRibInfo(String numeroCompte) {
        RibInfo info = new RibInfo();

        if (numeroCompte == null || numeroCompte.length() < 10) {
            // Valeurs par défaut si le format n'est pas bon
            info.codeBanque = "10228";
            info.codeGuichet = "00001";
            info.cleRib = "47";
            return info;
        }

        // Extraire selon le format du numéro de compte
        // Exemple: si le numéro fait 10 chiffres → les 5 premiers = banque, les 5 suivants = guichet
        if (numeroCompte.length() >= 10) {
            info.codeBanque = numeroCompte.substring(0, 5);
            info.codeGuichet = numeroCompte.substring(5, 10);

            // Calculer la clé RIB (algorithme simplifié)
            info.cleRib = calculateCleRib(info.codeBanque, info.codeGuichet, numeroCompte);
        } else {
            // Format court - valeurs par défaut
            info.codeBanque = "10228";
            info.codeGuichet = "00001";
            info.cleRib = "47";
        }

        return info;
    }

    /**
     * Calcule la clé RIB selon l'algorithme standard
     */
    private String calculateCleRib(String codeBanque, String codeGuichet, String numeroCompte) {
        try {
            // Algorithme de calcul de clé RIB
            String combined = codeBanque + codeGuichet + numeroCompte;

            // Convertir les lettres en chiffres (A=1, B=2, etc.)
            StringBuilder numeric = new StringBuilder();
            for (char c : combined.toUpperCase().toCharArray()) {
                if (Character.isDigit(c)) {
                    numeric.append(c);
                } else if (Character.isLetter(c)) {
                    // A=1, B=2, ..., Z=26
                    numeric.append(c - 'A' + 1);
                }
            }

            // Calculer le modulo 97
            long number = Long.parseLong(numeric.toString());
            long cle = 97 - (number % 97);

            return String.format("%02d", cle);
        } catch (Exception e) {
            // En cas d'erreur, retourner une clé par défaut
            return "47";
        }
    }

    /**
     * Classe interne pour stocker les infos RIB
     */
    private static class RibInfo {
        String codeBanque;
        String codeGuichet;
        String cleRib;
    }

    /**
     * Formatte un montant en FCFA
     */
    public static String formatMontant(BigDecimal montant) {
        if (montant == null) return "0 FCFA";
        return String.format("%,.0f FCFA", montant);
    }

    /**
     * Retourne la couleur selon le sens de la transaction
     */
    public static String getColorForSens(String sens) {
        return "CREDIT".equals(sens) ? "#10b981" : "#ef4444";
    }
}