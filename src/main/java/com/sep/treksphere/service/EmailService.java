package com.sep.treksphere.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendVerificationEmail(String to, String fullName, String verificationUrl) {
        log.info("Preparing to send verification email to {}", to);
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("verificationUrl", verificationUrl);

            String process = templateEngine.process("email-verification", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject("TrekSphere - Verify your email address");
            helper.setText(process, true);

            mailSender.send(mimeMessage);
            log.info("Verification email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
