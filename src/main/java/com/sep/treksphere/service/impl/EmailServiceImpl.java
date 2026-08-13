package com.sep.treksphere.service.impl;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.dto.email.BookingConfirmationEmailData;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final String BOOKING_LOGO_CID = "treksphere-logo";
    private static final String BOOKING_LOGO_RESOURCE = "/email/treksphere-mark-dark.png";

    private final TemplateEngine templateEngine;
    private final SendGrid sendGrid;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    @Value("${application.frontend.url}")
    private String frontendUrl;

    public EmailServiceImpl(TemplateEngine templateEngine, SendGrid sendGrid) {
        this.templateEngine = templateEngine;
        this.sendGrid = sendGrid;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        Context context = new Context();
        context.setVariable("resetLink", resetLink);

        sendEmail(toEmail, "TrekSphere - Yêu cầu đặt lại mật khẩu", "password-reset", context);
    }

    @Override
    public void sendVerificationEmail(String toAddress, String fullName, String verificationUrl) {
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("verificationUrl", verificationUrl);

        sendEmail(toAddress, "TrekSphere - Xác minh địa chỉ email của bạn", "email-verification", context);
    }

    @Override
    public void sendStaffInvitationEmail(String toEmail, String fullName, String companyName, String password, String activationUrl) {
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("companyName", companyName);
        context.setVariable("email", toEmail);
        context.setVariable("password", password);
        context.setVariable("activationUrl", activationUrl);

        sendEmail(toEmail, "TrekSphere - Lời mời tham gia doanh nghiệp " + companyName, "vendor-staff-invitation", context);
    }

    @Override
    public void sendBookingConfirmationEmail(BookingConfirmationEmailData data) {
        Context context = new Context();
        context.setVariable("recipientName", data.recipientName());
        context.setVariable("recipientEmail", data.recipientEmail());
        context.setVariable("bookingCode", data.bookingCode());
        context.setVariable("tourName", data.tourName());
        context.setVariable("departureDate", data.departureDate());
        context.setVariable("returnDate", data.returnDate());
        context.setVariable("duration", data.duration());
        context.setVariable("meetingPoint", data.meetingPoint());
        context.setVariable("participantCount", data.participantCount());
        context.setVariable("vendorName", data.vendorName());
        context.setVariable("totalPrice", data.totalPrice());
        context.setVariable("paidAmount", data.paidAmount());
        context.setVariable("paymentStatus", data.paymentStatus());
        context.setVariable("bookingDetailUrl", frontendUrl.replaceAll("/+$", "")
                + "/bookings/" + data.bookingId());

        sendEmail(data.recipientEmail(),
                "TrekSphere - Booking " + data.bookingCode() + " đã được xác nhận",
                "booking-confirmation", context, true);
    }

    private void sendEmail(String toAddress, String subject, String templateName, Context context) {
        sendEmail(toAddress, subject, templateName, context, false);
    }

    private void sendEmail(String toAddress, String subject, String templateName, Context context,
                           boolean includeBrandLogo) {
        log.info("Preparing to send email [{}] to {}", templateName, toAddress);
        try {
            String htmlContent = templateEngine.process(templateName, context);

            Email from = new Email(fromEmail, "TrekSphere");
            Email to = new Email(toAddress);
            Content content = new Content("text/html", htmlContent);
            Mail mail = new Mail(from, subject, to, content);
            if (includeBrandLogo) {
                mail.addAttachments(createInlineBrandLogo());
            }

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Email [{}] sent successfully to {}", templateName, toAddress);
            } else {
                log.error("SendGrid API returned status {}: {}", response.getStatusCode(), response.getBody());
                throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
            }
        } catch (IOException e) {
            log.error("Failed to send email [{}] to {}", templateName, toAddress, e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private Attachments createInlineBrandLogo() throws IOException {
        try (InputStream input = EmailServiceImpl.class.getResourceAsStream(BOOKING_LOGO_RESOURCE)) {
            if (input == null) {
                throw new IOException("TrekSphere email logo resource was not found");
            }
            Attachments logo = new Attachments();
            logo.setContent(Base64.getEncoder().encodeToString(input.readAllBytes()));
            logo.setType("image/png");
            logo.setFilename("treksphere-logo.png");
            logo.setDisposition("inline");
            logo.setContentId(BOOKING_LOGO_CID);
            return logo;
        }
    }
}
