package com.sep.treksphere.auth.service;

import com.sep.treksphere.auth.dto.request.LoginRequest;
import com.sep.treksphere.auth.dto.response.LoginResponse;
import com.sep.treksphere.auth.mapper.AuthMapper;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.common.security.JwtService;
import com.sep.treksphere.user.entity.User;
import com.sep.treksphere.user.enums.UserStatus;
import com.sep.treksphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthMapper authMapper;

    private AuthService authService;

    private User sampleUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                null,
                refreshTokenService,
                null,
                null,
                null,
                null,
                jwtService,
                null,
                authenticationManager,
                authMapper,
                null,
                null
        );

        sampleUser = new User();
        sampleUser.setUserId(UUID.randomUUID());
        sampleUser.setEmail("trekker@example.com");
        sampleUser.setPasswordHash("hashed-password");
        sampleUser.setStatus(UserStatus.ACTIVE);
        sampleUser.setEmailVerified(true);

        loginRequest = LoginRequest.builder()
                .email("trekker@example.com")
                .password("Pass123@")
                .build();
    }

    @Test
    @DisplayName("Đăng nhập thành công khi email/mật khẩu đúng và email đã xác thực")
    void login_Success() {
        when(userRepository.findByEmail("trekker@example.com")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh-token");
        when(jwtService.extractJti("refresh-token")).thenReturn("jti-123");

        LoginResponse expectedResponse = LoginResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();
        when(authMapper.toLoginResponse(sampleUser, "access-token", "refresh-token")).thenReturn(expectedResponse);

        LoginResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenService).store("trekker@example.com", "jti-123", 0L);
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi email không tồn tại -> Bắn AppException USER_NOT_FOUND")
    void login_UserNotFound() {
        when(userRepository.findByEmail("trekker@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi sai mật khẩu -> Ném ra lỗi xác thực từ AuthenticationManager")
    void login_WrongPassword_ThrowsAuthenticationException() {
        when(userRepository.findByEmail("trekker@example.com")).thenReturn(Optional.of(sampleUser));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tài khoản bị khóa -> Bắn AppException ACCOUNT_LOCKED")
    void login_AccountLocked_ThrowsAppException() {
        when(userRepository.findByEmail("trekker@example.com")).thenReturn(Optional.of(sampleUser));
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("Locked"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_LOCKED);
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tài khoản bị vô hiệu hóa -> Bắn AppException ACCOUNT_DEACTIVATED")
    void login_AccountDeactivated_ThrowsAppException() {
        when(userRepository.findByEmail("trekker@example.com")).thenReturn(Optional.of(sampleUser));
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("Disabled"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_DEACTIVATED);
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi email chưa được xác thực -> Bắn AppException EMAIL_NOT_VERIFIED")
    void login_EmailNotVerified_ThrowsAppException() {
        sampleUser.setEmailVerified(false);
        when(userRepository.findByEmail("trekker@example.com")).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_NOT_VERIFIED);

        verify(jwtService, never()).generateToken(any(CustomUserDetails.class));
    }
}
