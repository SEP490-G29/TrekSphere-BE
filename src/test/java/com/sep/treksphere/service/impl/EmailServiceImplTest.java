package com.sep.treksphere.service.impl;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sep.treksphere.dto.email.BookingConfirmationEmailData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock private TemplateEngine templateEngine;
    @Mock private SendGrid sendGrid;
    @Mock private Response sendGridResponse;

    @Test
    void bookingConfirmationUsesProfessionalTemplateAndBookingDetailLink() throws Exception {
        EmailServiceImpl service = new EmailServiceImpl(templateEngine, sendGrid);
        ReflectionTestUtils.setField(service, "fromEmail", "no-reply@treksphere.vn");
        ReflectionTestUtils.setField(service, "frontendUrl", "https://treksphere.vn/");

        UUID bookingId = UUID.randomUUID();
        BookingConfirmationEmailData data = new BookingConfirmationEmailData(
                bookingId, "BK-2026-001", "trekker@example.com", "Nguyễn Văn A",
                "Fansipan 2N1Đ", "15/09/2026", "16/09/2026", "2 ngày",
                "Sa Pa, Lào Cai", 2, "TrekSphere Adventure",
                "2.000.000 ₫", "2.000.000 ₫", "PAID");
        when(templateEngine.process(eq("booking-confirmation"), any(Context.class)))
                .thenReturn("<html>booking confirmation</html>");
        when(sendGridResponse.getStatusCode()).thenReturn(202);
        when(sendGrid.api(any(Request.class))).thenReturn(sendGridResponse);

        service.sendBookingConfirmationEmail(data);

        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("booking-confirmation"), context.capture());
        assertEquals("BK-2026-001", context.getValue().getVariable("bookingCode"));
        assertEquals("https://treksphere.vn/bookings/" + bookingId,
                context.getValue().getVariable("bookingDetailUrl"));
        ArgumentCaptor<Request> sendGridRequest = ArgumentCaptor.forClass(Request.class);
        verify(sendGrid).api(sendGridRequest.capture());
        assertTrue(sendGridRequest.getValue().getBody().contains("treksphere-logo"));
        assertTrue(sendGridRequest.getValue().getBody().contains("\"disposition\":\"inline\""));
    }

    @Test
    void bookingConfirmationTemplateRendersValidDynamicContent() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        TemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        Context context = new Context();
        context.setVariable("recipientName", "Nguyễn Văn A");
        context.setVariable("recipientEmail", "trekker@example.com");
        context.setVariable("bookingCode", "BK-2026-001");
        context.setVariable("tourName", "Fansipan 2N1Đ");
        context.setVariable("departureDate", "15/09/2026");
        context.setVariable("returnDate", "16/09/2026");
        context.setVariable("duration", "2 ngày");
        context.setVariable("meetingPoint", "Sa Pa, Lào Cai");
        context.setVariable("participantCount", 2);
        context.setVariable("vendorName", "TrekSphere Adventure");
        context.setVariable("totalPrice", "2.000.000 ₫");
        context.setVariable("paidAmount", "2.000.000 ₫");
        context.setVariable("paymentStatus", "PAID");
        context.setVariable("bookingDetailUrl", "https://treksphere.vn/bookings/booking-id");

        String rendered = engine.process("booking-confirmation", context);

        assertTrue(rendered.contains("BK-2026-001"));
        assertTrue(rendered.contains("Fansipan 2N1Đ"));
        assertTrue(rendered.contains("https://treksphere.vn/bookings/booking-id"));
        assertTrue(rendered.contains("cid:treksphere-logo"));
        assertFalse(rendered.contains("▲"));
        assertFalse(rendered.contains("th:text="));
    }
}
