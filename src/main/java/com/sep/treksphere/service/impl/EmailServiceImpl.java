package com.sep.treksphere.service.impl;

import com.sep.treksphere.service.EmailService;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
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
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("TrekSphere - Password Reset Request");
            
            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                        <h2 style="color: #2e6c80;">Password Reset Request</h2>
                        <p>Hello,</p>
                        <p>We received a request to reset the password for your TrekSphere account.</p>
                        <p>Click the button below to set a new password. This link is valid for 5 minutes.</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="background-color: #2e6c80; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;">Reset Password</a>
                        </div>
                        <p>If the button doesn't work, you can copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; color: #555;">%s</p>
                        <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;" />
                        <p style="font-size: 12px; color: #888;">If you didn't request a password reset, you can safely ignore this email.</p>
                        <p style="font-size: 12px; color: #888;">TrekSphere Team</p>
                    </div>
                    """.formatted(resetLink, resetLink);

            helper.setText(htmlContent, true); // true = isHtml

            mailSender.send(message);
            log.info("Password reset email sent successfully to {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}", toEmail, e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

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
            helper.setSubject("TrekSphere - Xác minh địa chỉ email của bạn");
            helper.setText(process, true);

            mailSender.send(mimeMessage);
            log.info("Verification email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}", to, e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
