package ega.api.egafinance.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    private final Resend resend;

    private final TemplateEngine templateEngine;

    @Value("${resend.from.email}")
    private String fromEmail;

    public EmailService(Resend resend, TemplateEngine templateEngine) {
        this.resend = resend;
        this.templateEngine = templateEngine;
    }


    public void sendEmail(String to, String subject, String htmlContent) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(to)
                .subject(subject)
                .html(htmlContent)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(params);
            System.out.println("Email envoyé avec succès, ID: " + response.getId());
        } catch (ResendException e) {

            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
    }


    public void sendWelcomeEmail(String to, String prenom, String identifiant) {

        Context context = new Context();
        context.setVariable("prenom", prenom);
        context.setVariable("identifiant", identifiant);


        String htmlContent = templateEngine.process("welcome-email", context);


        sendEmail(to, "Bienvenue parmi nous !", htmlContent);
    }


}