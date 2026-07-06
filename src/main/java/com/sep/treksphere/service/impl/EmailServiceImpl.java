package com.sep.treksphere.service.impl;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sep.treksphere.service.EmailService;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final TemplateEngine templateEngine;
    private final SendGrid sendGrid;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    public EmailServiceImpl(TemplateEngine templateEngine, @Value("${sendgrid.api-key}") String apiKey) {
        this.templateEngine = templateEngine;
        this.sendGrid = new SendGrid(apiKey);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        log.info("Preparing to send password reset email to {}", toEmail);
        try {
            Context context = new Context();
            context.setVariable("resetLink", resetLink);

            String htmlContent = templateEngine.process("password-reset", context);

            Email from = new Email(fromEmail, "TrekSphere");
            String subject = "TrekSphere - Password Reset Request";
            Email to = new Email(toEmail);
            Content content = new Content("text/html", htmlContent);
            Mail mail = new Mail(from, subject, to, content);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sendGrid.api(request);
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Password reset email sent successfully to {}", toEmail);
            } else {
                log.error("SendGrid API returned status {}: {}", response.getStatusCode(), response.getBody());
                throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
            }
        } catch (IOException e) {
            log.error("Failed to send password reset email to {}", toEmail, e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    @Override
    public void sendVerificationEmail(String toAddress, String fullName, String verificationUrl) {
        log.info("Preparing to send verification email to {}", toAddress);
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("verificationUrl", verificationUrl);

            String process = templateEngine.process("email-verification", context);

            Email from = new Email(fromEmail, "TrekSphere");
            String subject = "TrekSphere - Xác minh địa chỉ email của bạn";
            Email to = new Email(toAddress);
            Content content = new Content("text/html", process);
            Mail mail = new Mail(from, subject, to, content);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Verification email sent successfully to {}", toAddress);
            } else {
                log.error("SendGrid API returned status {}: {}", response.getStatusCode(), response.getBody());
                throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
            }
        } catch (IOException e) {
            log.error("Failed to send verification email to {}", toAddress, e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
