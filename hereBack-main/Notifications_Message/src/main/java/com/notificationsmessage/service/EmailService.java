package com.notificationsmessage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
@Retryable(
        value = { MailSendException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
)
@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Retryable(value = {MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendEmail(String to, String subject, String text, boolean isHtml) {
        logger.info("Tentative d'envoi d'email à {} avec sujet: {}", to, subject);

        if (to == null || to.trim().isEmpty()) {
            logger.error("Tentative d'envoi d'email à une adresse nulle ou vide");
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, isHtml);

            javaMailSender.send(message);
            logger.info("Email envoyé avec succès à {}", to);
        } catch (MessagingException e) {
            logger.error("Erreur lors de la création du message pour {}: {}", to, e.getMessage());
            throw new MailException("Failed to create message", e) {};
        } catch (MailException me) {
            logger.error("Erreur lors de l'envoi de l'email à {}: {}", to, me.getMessage());
            throw me;
        }
    }
}

