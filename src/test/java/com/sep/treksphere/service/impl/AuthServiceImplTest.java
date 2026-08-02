package com.sep.treksphere.service.impl;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.LoginRequest;
import com.sep.treksphere.dto.request.RegisterRequest;
import com.sep.treksphere.dto.response.LoginResponse;
import com.sep.treksphere.dto.response.RegisterResponse;
import com.sep.treksphere.entity.Role;
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
import com.sep.treksphere.service.EmailVerificationRateLimiter;
import com.sep.treksphere.service.RefreshTokenService;
import com.sep.treksphere.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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
    void UTCID04_login_Fail_AccountLocked() {
        // Arrange
        mockUser.setStatus(UserStatus.LOCKED);
        when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new LockedException("User account is locked"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_LOCKED)
                .hasMessage(MessageConstant.ACCOUNT_LOCKED);

        verify(userRepository, times(1)).findByEmail(validLoginRequest.getEmail());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(jwtService, refreshTokenService, authMapper);
    }

    @Test
    void UTCID05_login_Fail_AccountDeactivated() {
        // Arrange
        mockUser.setStatus(UserStatus.DEACTIVATED);
        when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_DEACTIVATED)
                .hasMessage(MessageConstant.ACCOUNT_DEACTIVATED);

        verify(userRepository, times(1)).findByEmail(validLoginRequest.getEmail());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(jwtService, refreshTokenService, authMapper);
    }

    @Test
    void UTCID06_login_Fail_EmailNotVerified() {
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

    @Test
    void customUserDetails_MapsAccountStatusesCorrectly() {
        mockUser.setStatus(UserStatus.ACTIVE);
        CustomUserDetails activeUser = new CustomUserDetails(mockUser);
        assertThat(activeUser.isAccountNonLocked()).isTrue();
        assertThat(activeUser.isEnabled()).isTrue();

        mockUser.setStatus(UserStatus.LOCKED);
        CustomUserDetails lockedUser = new CustomUserDetails(mockUser);
        assertThat(lockedUser.isAccountNonLocked()).isFalse();
        assertThat(lockedUser.isEnabled()).isTrue();

        mockUser.setStatus(UserStatus.DEACTIVATED);
        CustomUserDetails deactivatedUser = new CustomUserDetails(mockUser);
        assertThat(deactivatedUser.isAccountNonLocked()).isTrue();
        assertThat(deactivatedUser.isEnabled()).isFalse();
    }

    @Test
    void resendVerificationEmail_SendsNewLink_ForUnverifiedActiveUser() {
        mockUser.setEmailVerified(false);
        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(tokenProvider.generateVerificationToken(mockUser.getEmail())).thenReturn("new_verification_token");
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:3000");

        authService.resendVerificationEmail(mockUser.getEmail());

        verify(emailVerificationRateLimiter).checkAllowed(mockUser.getEmail());
        verify(emailService).sendVerificationEmail(
                mockUser.getEmail(),
                mockUser.getFullName(),
                "http://localhost:3000/verify?token=new_verification_token"
        );
    }

    @Test
    void resendVerificationEmail_DoesNotSend_ForVerifiedUser() {
        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));

        authService.resendVerificationEmail(mockUser.getEmail());

        verify(emailVerificationRateLimiter).checkAllowed(mockUser.getEmail());
        verifyNoInteractions(tokenProvider, emailService);
    }

    @Test
    void verifyEmail_ReturnsSpecificError_WhenTokenExpired() {
        String expiredToken = "expired_token";
        when(tokenProvider.getEmailFromToken(expiredToken)).thenThrow(
                new ExpiredJwtException(mock(Header.class), mock(Claims.class), "expired")
        );

        assertThatThrownBy(() -> authService.verifyEmail(expiredToken))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_TOKEN_EXPIRED)
                .hasMessage(MessageConstant.VERIFICATION_TOKEN_EXPIRED);

        verifyNoInteractions(userRepository);
    }

    @Test
    void UTCID06_register_Success_WithValidData() {
        // Arrange (Thiết lập dữ liệu đầu vào và các giả lập)
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@treksphere.com")
                .password("Password@123")
                .confirmPassword("Password@123")
                .fullName("New User")
                .build();

        Role trekkerRole = new Role();
        trekkerRole.setRoleId(UUID.randomUUID());
        trekkerRole.setRoleName("TREKKER");

        // Sử dụng ReflectionTestUtils để tiêm giá trị frontendUrl do Mockito không tự tiêm từ config
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:3000");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByRoleName("TREKKER")).thenReturn(Optional.of(trekkerRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUserId(UUID.randomUUID());
            return u;
        });
        when(tokenProvider.generateVerificationToken(request.getEmail())).thenReturn("verification_token");
        doNothing().when(emailService).sendVerificationEmail(eq(request.getEmail()), eq(request.getFullName()), anyString());

        // Act (Thực hiện hành động đăng ký)
        RegisterResponse response = authService.register(request);

        // Assert (Kiểm tra kết quả trả về và các tương tác với mock)
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getFullName()).isEqualTo(request.getFullName());
        assertThat(response.getUserId()).isNotNull();

        verify(userRepository, times(1)).existsByEmail(request.getEmail());
        verify(roleRepository, times(1)).findByRoleName("TREKKER");
        verify(passwordEncoder, times(1)).encode(request.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenProvider, times(1)).generateVerificationToken(request.getEmail());
        verify(emailService, times(1)).sendVerificationEmail(eq(request.getEmail()), eq(request.getFullName()), eq("http://localhost:3000/verify?token=verification_token"));
    }

    @Test
    void UTCID07_register_Fail_PasswordNotMatch() {
        // Arrange (Thiết lập yêu cầu đăng ký có mật khẩu nhập lại không khớp)
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@treksphere.com")
                .password("Password@123")
                .confirmPassword("Password@1234")
                .fullName("New User")
                .build();

        // Act & Assert (Kiểm tra ngoại lệ ném ra lỗi VALIDATION_ERROR)
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR)
                .hasMessageContaining(MessageConstant.CONFIRM_PASSWORD_NOT_MATCH);

        // Đảm bảo không tương tác với các Mock dịch vụ khác sau khi lỗi
        verifyNoInteractions(userRepository, roleRepository, passwordEncoder, tokenProvider, emailService);
    }

    @Test
    void UTCID08_register_Fail_EmailExisted() {
        // Arrange (Thiết lập email đăng ký đã tồn tại trong database)
        RegisterRequest request = RegisterRequest.builder()
                .email("existed@treksphere.com")
                .password("Password@123")
                .confirmPassword("Password@123")
                .fullName("Existed User")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert (Kiểm tra ngoại lệ ném ra lỗi EMAIL_EXISTED)
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_EXISTED);

        verify(userRepository, times(1)).existsByEmail(request.getEmail());
        verifyNoInteractions(roleRepository, passwordEncoder, tokenProvider, emailService);
    }

    @Test
    void UTCID09_register_Fail_RoleNotFound() {
        // Arrange (Giả lập database thiếu cấu hình role mặc định 'TREKKER')
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@treksphere.com")
                .password("Password@123")
                .confirmPassword("Password@123")
                .fullName("New User")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByRoleName("TREKKER")).thenReturn(Optional.empty());

        // Act & Assert (Kiểm tra ngoại lệ ném ra lỗi ROLE_NOT_FOUND)
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_NOT_FOUND);

        verify(userRepository, times(1)).existsByEmail(request.getEmail());
        verify(roleRepository, times(1)).findByRoleName("TREKKER");
        verifyNoInteractions(passwordEncoder, tokenProvider, emailService);
    }

    @Test
    void UTCID10_register_Success_EvenIfEmailSendingFails() {
        // Arrange (Thiết lập giả lập khi gửi email xác thực gặp lỗi/exception)
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@treksphere.com")
                .password("Password@123")
                .confirmPassword("Password@123")
                .fullName("New User")
                .build();

        Role trekkerRole = new Role();
        trekkerRole.setRoleId(UUID.randomUUID());
        trekkerRole.setRoleName("TREKKER");

        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:3000");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByRoleName("TREKKER")).thenReturn(Optional.of(trekkerRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUserId(UUID.randomUUID());
            return u;
        });
        when(tokenProvider.generateVerificationToken(request.getEmail())).thenReturn("verification_token");
        
        // Ném ngoại lệ khi gửi mail nhưng không làm gián đoạn quá trình đăng ký
        doThrow(new RuntimeException("SMTP Server Down")).when(emailService).sendVerificationEmail(eq(request.getEmail()), eq(request.getFullName()), anyString());

        // Act (Đăng ký thành công dù gửi mail lỗi)
        RegisterResponse response = authService.register(request);

        // Assert (Kiểm tra đăng ký vẫn thành công và kết quả đúng mong đợi)
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getFullName()).isEqualTo(request.getFullName());
        assertThat(response.getUserId()).isNotNull();

        verify(userRepository, times(1)).existsByEmail(request.getEmail());
        verify(roleRepository, times(1)).findByRoleName("TREKKER");
        verify(passwordEncoder, times(1)).encode(request.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenProvider, times(1)).generateVerificationToken(request.getEmail());
        verify(emailService, times(1)).sendVerificationEmail(eq(request.getEmail()), eq(request.getFullName()), eq("http://localhost:3000/verify?token=verification_token"));
    }
}
