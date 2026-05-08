package org.unihubworkshop.notificationservice.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.unihubworkshop.notificationservice.dto.RegistrationConfirmedEvent;
import org.unihubworkshop.notificationservice.service.QRCodeService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Locale;

/**
 * Service for sending notification emails using Thymeleaf templates.
 * Handles registration confirmations and other email communications.
 */
@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String QR_CODE_IMAGE_ID = "qrCodeImage";

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final QRCodeService qrCodeService;

    @Value("${spring.mail.from}")
    private String mailFrom;

    @Value("${spring.mail.from-name:UniHub Workshop}")
    private String mailFromName;

    /**
     * Sends a registration confirmation email to the user using Thymeleaf template.
     *
     * @param event the registration confirmed event containing workshop and user details
     */
    public void sendRegistrationConfirmation(RegistrationConfirmedEvent event) {
        try {
            // Generate QR code as byte array
            byte[] qrCodeBytes = qrCodeService.generateQRCode(event.registrationId());

            // Prepare Thymeleaf context with event data
            Context context = new Context(Locale.of("vi", "VN"));
            context.setVariable("registrationId", event.registrationId().toString());
            context.setVariable("workshopName", event.workshopName());
            context.setVariable("speakerNames", event.speakerNames());
            context.setVariable("room", event.room());
            context.setVariable("roomMap", event.roomMap());
            context.setVariable("qrCodeImageId", QR_CODE_IMAGE_ID);

            // Process template with context
            String htmlContent = templateEngine.process("registration-confirmation", context);

            // Create and send email
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(mailFrom, mailFromName);
            helper.setTo(event.userEmail());
            helper.setSubject(String.format("Xác nhận đăng ký: %s", event.workshopName()));
            helper.setText(htmlContent, true); // true = HTML content

            // Attach QR code image inline
            if (qrCodeBytes != null && qrCodeBytes.length > 0) {
                ByteArrayResource qrCodeResource = new ByteArrayResource(qrCodeBytes);
                helper.addInline(QR_CODE_IMAGE_ID, qrCodeResource, "image/png");
            }

            javaMailSender.send(mimeMessage);
            log.info("Registration confirmation email sent to: {} for workshop: {}", 
                    event.userEmail(), event.workshopName());
        } catch (MessagingException e) {
            log.error("Failed to send registration confirmation email to: {}", event.userEmail(), e);
            throw new RuntimeException("Failed to send registration confirmation email", e);
        } catch (Exception e) {
            log.error("Unexpected error while sending registration confirmation email", e);
            throw new RuntimeException("Unexpected error while sending email", e);
        }
    }
}


