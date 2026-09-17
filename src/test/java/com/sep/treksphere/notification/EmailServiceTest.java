package com.sep.treksphere.notification;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Bao phủ 2 method mà `unit_test_gap_report.md` liệt kê thiếu: `sendPasswordResetEmail`,
 * `sendVerificationEmail`. `sendStaffInvitationEmail` bị loại vì 🚫 Không cần test (chết cả 2
 * phía — không tồn tại tính năng mời vendor staff nào).
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private TemplateEngine templateEngine;
    @Mock
    private SendGrid sendGrid;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(templateEngine, sendGrid);
    }

    private Response okResponse() throws IOException {
        Response response = mock(Response.class);
        when(response.getStatusCode()).thenReturn(202);
        return response;
    }

    @Test
    @DisplayName("sendPasswordResetEmail: SendGrid trả 2xx -> gửi thành công, không ném lỗi")
    void sendPasswordResetEmail_Success() throws IOException {
        Response response = okResponse();
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<html>reset</html>");
        when(sendGrid.api(any(Request.class))).thenReturn(response);

        assertThatCode(() -> emailService.sendPasswordResetEmail("user@example.com", "https://app/reset?token=abc"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendPasswordResetEmail: SendGrid trả lỗi (>=300) -> ném AppException EMAIL_SEND_FAILED")
    void sendPasswordResetEmail_SendGridErrorStatus_ThrowsAppException() throws IOException {
        Response failed = mock(Response.class);
        when(failed.getStatusCode()).thenReturn(500);
        when(failed.getBody()).thenReturn("Internal Server Error");
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<html>reset</html>");
        when(sendGrid.api(any(Request.class))).thenReturn(failed);

        assertThatThrownBy(() -> emailService.sendPasswordResetEmail("user@example.com", "https://app/reset?token=abc"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.getErrorCode())
                                .isEqualTo(ErrorCode.EMAIL_SEND_FAILED));
    }

    @Test
    @DisplayName("sendPasswordResetEmail: SendGrid ném IOException -> bọc thành AppException EMAIL_SEND_FAILED")
    void sendPasswordResetEmail_IOException_ThrowsAppException() throws IOException {
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<html>reset</html>");
        when(sendGrid.api(any(Request.class))).thenThrow(new IOException("network error"));

        assertThatThrownBy(() -> emailService.sendPasswordResetEmail("user@example.com", "https://app/reset?token=abc"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.getErrorCode())
                                .isEqualTo(ErrorCode.EMAIL_SEND_FAILED));
    }

    @Test
    @DisplayName("sendVerificationEmail: SendGrid trả 2xx -> gửi thành công, không ném lỗi")
    void sendVerificationEmail_Success() throws IOException {
        Response response = okResponse();
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<html>verify</html>");
        when(sendGrid.api(any(Request.class))).thenReturn(response);

        assertThatCode(() -> emailService.sendVerificationEmail(
                "user@example.com", "Nguyen Van A", "https://app/verify?token=abc"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendVerificationEmail: SendGrid trả lỗi -> ném AppException EMAIL_SEND_FAILED")
    void sendVerificationEmail_SendGridErrorStatus_ThrowsAppException() throws IOException {
        Response failed = mock(Response.class);
        when(failed.getStatusCode()).thenReturn(400);
        when(failed.getBody()).thenReturn("Bad Request");
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<html>verify</html>");
        when(sendGrid.api(any(Request.class))).thenReturn(failed);

        assertThatThrownBy(() -> emailService.sendVerificationEmail(
                "user@example.com", "Nguyen Van A", "https://app/verify?token=abc"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.getErrorCode())
                                .isEqualTo(ErrorCode.EMAIL_SEND_FAILED));
    }
}
