package org.unihubworkshop.notificationservice.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.unihubworkshop.notificationservice.dto.RegistrationConfirmedEvent;
import org.unihubworkshop.notificationservice.service.QRCodeService;

@Component
public class EmailNotificationStrategy implements NotificationStrategy {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationStrategy.class);
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final QRCodeService qrCodeService;

    public EmailNotificationStrategy(JavaMailSender mailSender, TemplateEngine templateEngine, QRCodeService qrCodeService) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.qrCodeService = qrCodeService;
    }

    @Override
    public void sendNotification(RegistrationConfirmedEvent event) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo("user-" + event.userId() + "@example.com");
            helper.setSubject("Xác nhận đăng ký workshop: " + event.workshopName());
            
            String qrCodeBase64 = qrCodeService.generateQRCodeBase64(event.registrationId());
            
            Context context = new Context();
            context.setVariable("workshopName", event.workshopName());
            context.setVariable("registrationId", event.registrationId());
            context.setVariable("qrCodeBase64", qrCodeBase64);
            context.setVariable("speakerNames", event.speakerNames());
            context.setVariable("room", event.room());
            context.setVariable("roomMap", event.roomMap());
            String html = templateEngine.process("registration-confirmation", context);
            
            helper.setText(html, true);
            mailSender.send(message);
            
            log.info("Email sent to user: {} for workshop: {}", event.userId(), event.workshopName());
        } catch (Exception e) {
            log.error("Failed to send email", e);
        }
    }
}
