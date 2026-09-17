package com.sep.treksphere.auth;

import com.sep.treksphere.auth.dto.response.LoginResponse;
import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.common.security.JwtService;
import com.sep.treksphere.common.security.JwtTokenProvider;
import com.sep.treksphere.notification.EmailService;
import com.sep.treksphere.user.RoleRepository;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.user.UserStatus;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Chỉ bao phủ 5 method mà `unit_test_gap_report.md` liệt kê thiếu:
 * verifyEmail, resendVerificationEmail, refreshToken, googleLogin, logout.
 * 5 method còn lại của AuthService (login/register/forgotPassword/changePassword/resetPassword)
 * được xem là đã có test ở nơi khác, không viết lại ở đây.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private EmailService emailService;
    @Mock
    private EmailVerificationRateLimiter emailVerificationRateLimiter;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AuthMapper authMapper;
    @Mock
    private ForgotPasswordRateLimiter forgotPasswordRateLimiter;
    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AuthService authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setUserId(UUID.randomUUID());
        activeUser.setEmail("trekker@example.com");
        activeUser.setFullName("Nguyen Van Trekker");
        activeUser.setPasswordHash("hashed");
        activeUser.setStatus(UserStatus.ACTIVE);
        activeUser.setEmailVerified(false);
    }

    // ---------------------------------------------------------------
    // verifyEmail
    // ---------------------------------------------------------------

    @Test
    @DisplayName("verifyEmail: token hợp lệ, user chưa xác minh -> đánh dấu đã xác minh")
    void verifyEmail_ValidToken_MarksEmailVerified() {
        when(tokenProvider.getEmailFromToken("valid-token")).thenReturn(activeUser.getEmail());
        when(userRepository.findByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));

        String result = authService.verifyEmail("valid-token");

        assertThat(result).isEqualTo(MessageConstant.EMAIL_VERIFIED_SUCCESSFULLY);
        assertThat(activeUser.isEmailVerified()).isTrue();
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("verifyEmail: user đã xác minh từ trước -> trả về thông báo đã xác minh, không save lại")
    void verifyEmail_AlreadyVerified_ReturnsAlreadyVerifiedMessage() {
        activeUser.setEmailVerified(true);
        when(tokenProvider.getEmailFromToken("valid-token")).thenReturn(activeUser.getEmail());
        when(userRepository.findByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));

        String result = authService.verifyEmail("valid-token");

        assertThat(result).isEqualTo(MessageConstant.EMAIL_ALREADY_VERIFIED);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("verifyEmail: token hết hạn -> ném AppException VERIFICATION_TOKEN_EXPIRED")
    void verifyEmail_ExpiredToken_ThrowsVerificationTokenExpired() {
        when(tokenProvider.getEmailFromToken("expired-token"))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));

        assertThatThrownBy(() -> authService.verifyEmail("expired-token"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VERIFICATION_TOKEN_EXPIRED));
    }

    @Test
    @DisplayName("verifyEmail: token không hợp lệ -> ném AppException INVALID_TOKEN")
    void verifyEmail_InvalidToken_ThrowsInvalidToken() {
        when(tokenProvider.getEmailFromToken("garbage")).thenThrow(new JwtException("malformed"));

        assertThatThrownBy(() -> authService.verifyEmail("garbage"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("verifyEmail: token hợp lệ nhưng user không tồn tại -> ném AppException USER_NOT_FOUND")
    void verifyEmail_UserNotFound_ThrowsUserNotFound() {
        when(tokenProvider.getEmailFromToken("valid-token")).thenReturn("ghost@example.com");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("valid-token"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // resendVerificationEmail
    // ---------------------------------------------------------------

    @Test
    @DisplayName("resendVerificationEmail: user không tồn tại -> âm thầm bỏ qua, không gửi email")
    void resendVerificationEmail_UserNotFound_ReturnsSilently() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        authService.resendVerificationEmail("ghost@example.com");

        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
    }

    @Test
    @DisplayName("resendVerificationEmail: user đã xác minh -> âm thầm bỏ qua, không gửi email")
    void resendVerificationEmail_AlreadyVerified_ReturnsSilently() {
        activeUser.setEmailVerified(true);
        when(userRepository.findByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));

        authService.resendVerificationEmail(activeUser.getEmail());

        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
    }

    @Test
    @DisplayName("resendVerificationEmail: user không ACTIVE (đã khoá) -> âm thầm bỏ qua")
    void resendVerificationEmail_UserNotActive_ReturnsSilently() {
        activeUser.setStatus(UserStatus.LOCKED);
        when(userRepository.findByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));

        authService.resendVerificationEmail(activeUser.getEmail());

        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
    }

    @Test
    @DisplayName("resendVerificationEmail: user hợp lệ -> tạo token mới và gửi lại email xác minh")
    void resendVerificationEmail_ValidUser_SendsVerificationEmail() {
        when(userRepository.findByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));
        when(tokenProvider.generateVerificationToken(activeUser.getEmail())).thenReturn("new-token");

        authService.resendVerificationEmail(activeUser.getEmail());

        verify(emailService).sendVerificationEmail(
                eq(activeUser.getEmail()), eq(activeUser.getFullName()), any());
    }

    @Test
    @DisplayName("resendVerificationEmail: bị giới hạn tần suất -> ném lỗi ngay, không tra user")
    void resendVerificationEmail_RateLimited_PropagatesException() {
        org.mockito.Mockito.doThrow(new AppException(ErrorCode.VERIFICATION_RESEND_RATE_LIMITED))
                .when(emailVerificationRateLimiter).checkAllowed(activeUser.getEmail());

        assertThatThrownBy(() -> authService.resendVerificationEmail(activeUser.getEmail()))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VERIFICATION_RESEND_RATE_LIMITED));

        verify(userRepository, never()).findByEmail(any());
    }

    // ---------------------------------------------------------------
    // refreshToken
    // ---------------------------------------------------------------

    @Test
    @DisplayName("refreshToken: chuỗi rỗng -> ném AppException INVALID_TOKEN")
    void refreshToken_BlankToken_ThrowsInvalidToken() {
        assertThatThrownBy(() -> authService.refreshToken("  "))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("refreshToken: sai chữ ký -> ném AppException INVALID_TOKEN")
    void refreshToken_InvalidSignature_ThrowsInvalidToken() {
        when(jwtService.isSignatureValid("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("bad-token"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("refreshToken: token đúng chữ ký nhưng sai type (không phải refresh) -> INVALID_TOKEN")
    void refreshToken_WrongType_ThrowsInvalidToken() {
        when(jwtService.isSignatureValid("access-token")).thenReturn(true);
        when(jwtService.extractType("access-token")).thenReturn("access");

        assertThatThrownBy(() -> authService.refreshToken("access-token"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("refreshToken: jti đã bị blacklist (token cũ bị dùng lại) -> INVALID_TOKEN")
    void refreshToken_BlacklistedJti_ThrowsInvalidToken() {
        when(jwtService.isSignatureValid("refresh-token")).thenReturn(true);
        when(jwtService.extractType("refresh-token")).thenReturn("refresh");
        when(jwtService.extractJti("refresh-token")).thenReturn("jti-1");
        when(tokenBlacklistService.isBlacklisted("jti-1")).thenReturn(true);

        assertThatThrownBy(() -> authService.refreshToken("refresh-token"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("refreshToken: không tìm thấy user theo email trong token -> INVALID_TOKEN")
    void refreshToken_UserNotFound_ThrowsInvalidToken() {
        when(jwtService.isSignatureValid("refresh-token")).thenReturn(true);
        when(jwtService.extractType("refresh-token")).thenReturn("refresh");
        when(jwtService.extractJti("refresh-token")).thenReturn("jti-1");
        when(tokenBlacklistService.isBlacklisted("jti-1")).thenReturn(false);
        when(jwtService.extractUsername("refresh-token")).thenReturn("ghost@example.com");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("refresh-token"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("refreshToken: tài khoản đang bị khoá -> ném AppException ACCOUNT_LOCKED")
    void refreshToken_AccountLocked_ThrowsAccountLocked() {
        activeUser.setStatus(UserStatus.LOCKED);
        when(jwtService.isSignatureValid("refresh-token")).thenReturn(true);
        when(jwtService.extractType("refresh-token")).thenReturn("refresh");
        when(jwtService.extractJti("refresh-token")).thenReturn("jti-1");
        when(tokenBlacklistService.isBlacklisted("jti-1")).thenReturn(false);
        when(jwtService.extractUsername("refresh-token")).thenReturn(activeUser.getEmail());
        when(userRepository.findByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.refreshToken("refresh-token"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED));
    }

    @Test
    @DisplayName("refreshToken: JwtService xác nhận token không còn hợp lệ -> INVALID_TOKEN")
    void refreshToken_TokenNotValidPerJwtService_ThrowsInvalidToken() {
        when(jwtService.isSignatureValid("refresh-token")).thenReturn(true);
        when(jwtService.extractType("refresh-token")).thenReturn("refresh");
        when(jwtService.extractJti("refresh-token")).thenReturn("jti-1");
        when(tokenBlacklistService.isBlacklisted("jti-1")).thenReturn(false);
        when(jwtService.extractUsername("refresh-token")).thenReturn(activeUser.getEmail());
        when(userRepository.findByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));
        when(jwtService.isTokenValid(eq("refresh-token"), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("refresh-token"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("refreshToken: hợp lệ toàn bộ -> blacklist token cũ, cấp cặp token mới")
    void refreshToken_Success_ReturnsLoginResponseAndRotatesToken() {
        when(jwtService.isSignatureValid("refresh-token")).thenReturn(true);
        when(jwtService.extractType("refresh-token")).thenReturn("refresh");
        when(jwtService.extractJti("refresh-token")).thenReturn("old-jti");
        when(tokenBlacklistService.isBlacklisted("old-jti")).thenReturn(false);
        when(jwtService.extractUsername("refresh-token")).thenReturn(activeUser.getEmail());
        when(userRepository.findByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));
        when(jwtService.isTokenValid(eq("refresh-token"), any())).thenReturn(true);
        when(jwtService.extractExpiration("refresh-token"))
                .thenReturn(new Date(System.currentTimeMillis() + 60_000));
        when(jwtService.generateToken(any())).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh-token");
        when(jwtService.extractJti("new-refresh-token")).thenReturn("new-jti");
        LoginResponse expected = LoginResponse.builder().accessToken("new-access-token").build();
        when(authMapper.toLoginResponse(eq(activeUser), eq("new-access-token"), eq("new-refresh-token")))
                .thenReturn(expected);

        LoginResponse result = authService.refreshToken("refresh-token");

        assertThat(result).isEqualTo(expected);
        verify(tokenBlacklistService).blacklist(eq("old-jti"), anyLong());
        verify(refreshTokenService).store(eq(activeUser.getEmail()), eq("new-jti"), anyLong());
    }

    // ---------------------------------------------------------------
    // googleLogin
    // ---------------------------------------------------------------

    @Test
    @DisplayName("googleLogin: id-token không đúng định dạng JWT -> bọc lại thành AppException INVALID_TOKEN")
    void googleLogin_MalformedToken_ThrowsAppException() {
        // GoogleIdTokenVerifier được khởi tạo ngay trong AuthService (không inject được),
        // nên chỉ unit-test được nhánh parse lỗi cục bộ (không gọi mạng) — nhánh happy-path
        // (id-token thật hợp lệ từ Google) không kiểm được bằng unit test thuần, cần
        // integration/manual test riêng.
        assertThatThrownBy(() -> authService.googleLogin("not-a-valid-jwt"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));

        verifyNoInteractions(userRepository);
    }

    // ---------------------------------------------------------------
    // logout
    // ---------------------------------------------------------------

    @Test
    @DisplayName("logout: access + refresh token hợp lệ -> blacklist cả 2 và thu hồi phiên refresh")
    void logout_ValidAccessAndRefreshTokens_BlacklistsBoth() {
        when(jwtService.isSignatureValid("access-token")).thenReturn(true);
        when(jwtService.extractType("access-token")).thenReturn("access");
        when(jwtService.extractJti("access-token")).thenReturn("access-jti");
        when(jwtService.extractExpiration("access-token"))
                .thenReturn(new Date(System.currentTimeMillis() + 60_000));

        when(jwtService.isSignatureValid("refresh-token")).thenReturn(true);
        when(jwtService.extractType("refresh-token")).thenReturn("refresh");
        when(jwtService.extractJti("refresh-token")).thenReturn("refresh-jti");
        when(jwtService.extractExpiration("refresh-token"))
                .thenReturn(new Date(System.currentTimeMillis() + 60_000));
        when(jwtService.extractUsername("refresh-token")).thenReturn(activeUser.getEmail());

        authService.logout("access-token", "refresh-token");

        verify(tokenBlacklistService).blacklist(eq("access-jti"), anyLong());
        verify(tokenBlacklistService).blacklist(eq("refresh-jti"), anyLong());
        verify(refreshTokenService).revokeAll(activeUser.getEmail());
    }

    @Test
    @DisplayName("logout: cả 2 token đều null -> không có tương tác nào, không ném lỗi")
    void logout_NullTokens_NoInteractions() {
        authService.logout(null, null);

        verifyNoInteractions(tokenBlacklistService);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    @DisplayName("logout: access token sai chữ ký -> bỏ qua blacklist cho access token")
    void logout_InvalidAccessTokenSignature_SkipsBlacklist() {
        when(jwtService.isSignatureValid("bad-access-token")).thenReturn(false);

        authService.logout("bad-access-token", null);

        verifyNoInteractions(tokenBlacklistService);
    }

    @Test
    @DisplayName("logout: thu hồi refresh session lỗi -> nuốt lỗi, không ném ra ngoài")
    void logout_RefreshTokenRevokeThrows_SwallowsException() {
        when(jwtService.isSignatureValid("access-token")).thenReturn(true);
        when(jwtService.extractType("access-token")).thenReturn("access");
        when(jwtService.extractJti("access-token")).thenReturn("access-jti");
        when(jwtService.extractExpiration("access-token"))
                .thenReturn(new Date(System.currentTimeMillis() + 60_000));

        when(jwtService.isSignatureValid("refresh-token")).thenReturn(true);
        when(jwtService.extractType("refresh-token")).thenReturn("refresh");
        when(jwtService.extractJti("refresh-token")).thenReturn("refresh-jti");
        when(jwtService.extractExpiration("refresh-token"))
                .thenReturn(new Date(System.currentTimeMillis() + 60_000));
        when(jwtService.extractUsername("refresh-token")).thenThrow(new RuntimeException("boom"));

        org.assertj.core.api.Assertions.assertThatCode(() -> authService.logout("access-token", "refresh-token"))
                .doesNotThrowAnyException();

        verify(refreshTokenService, never()).revokeAll(any());
    }
}
