package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.LoginRequest;
import com.sep.treksphere.dto.response.LoginResponse;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.user.UserStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.AuthMapper;
import com.sep.treksphere.repository.RoleRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.security.JwtService;
import com.sep.treksphere.security.JwtTokenProvider;
import com.sep.treksphere.service.EmailService;
import com.sep.treksphere.service.RefreshTokenService;
import com.sep.treksphere.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

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
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuthMapper authMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginRequest validLoginRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequest();
        validLoginRequest.setEmail("trekker@treksphere.com");
        validLoginRequest.setPassword("Password@123");

        mockUser = new User();
        mockUser.setUserId(UUID.randomUUID());
        mockUser.setEmail("trekker@treksphere.com");
        mockUser.setPasswordHash("encoded_password");
        mockUser.setStatus(UserStatus.ACTIVE);
        mockUser.setEmailVerified(true);
    }

    @Test
    void UTCID01_login_Success_WithValidCredentials() {
        // Arrange
        when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("refresh_token");
        when(jwtService.extractJti("refresh_token")).thenReturn("jti_id");
        doNothing().when(refreshTokenService).store(anyString(), anyString(), anyLong());

        LoginResponse mockResponse = LoginResponse.builder()
                .accessToken("access_token")
                .refreshToken("refresh_token")
                .build();
        when(authMapper.toLoginResponse(eq(mockUser), eq("access_token"), eq("refresh_token"))).thenReturn(mockResponse);

        // Act
        LoginResponse response = authService.login(validLoginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");

        verify(userRepository, times(1)).findByEmail(validLoginRequest.getEmail());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateToken(any(CustomUserDetails.class));
        verify(jwtService, times(1)).generateRefreshToken(any(CustomUserDetails.class));
        verify(refreshTokenService, times(1)).store(eq(mockUser.getEmail()), eq("jti_id"), anyLong());
    }

    @Test
    void UTCID02_login_Fail_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(userRepository, times(1)).findByEmail(validLoginRequest.getEmail());
        verifyNoInteractions(authenticationManager, jwtService, refreshTokenService, authMapper);
    }

    @Test
    void UTCID03_login_Fail_BadCredentials() {
        // Arrange
        when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, times(1)).findByEmail(validLoginRequest.getEmail());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(jwtService, refreshTokenService, authMapper);
    }

    @Test
    void UTCID04_login_Fail_UserNotActive() {
        // Arrange
        mockUser.setStatus(UserStatus.LOCKED);
        when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_ACTIVE);

        verify(userRepository, times(1)).findByEmail(validLoginRequest.getEmail());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(jwtService, refreshTokenService, authMapper);
    }

    @Test
    void UTCID05_login_Fail_EmailNotVerified() {
        // Arrange
        mockUser.setEmailVerified(false);
        when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_NOT_VERIFIED);

        verify(userRepository, times(1)).findByEmail(validLoginRequest.getEmail());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(jwtService, refreshTokenService, authMapper);
    }
}
