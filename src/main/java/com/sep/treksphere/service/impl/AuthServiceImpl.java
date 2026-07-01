package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.AuthRequest;
import com.sep.treksphere.dto.request.RegisterRequest;
import com.sep.treksphere.dto.response.AuthResponse;
import com.sep.treksphere.dto.response.RegisterResponse;
import com.sep.treksphere.dto.response.UserResponse;
import com.sep.treksphere.entity.RefreshToken;
import com.sep.treksphere.entity.Role;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.user.TokenStatus;
import com.sep.treksphere.enums.user.UserStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.RefreshTokenRepository;
import com.sep.treksphere.repository.RoleRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.security.JwtService;
import com.sep.treksphere.security.JwtTokenProvider;
import com.sep.treksphere.service.AuthService;
import com.sep.treksphere.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    @Override
    @Transactional
    public AuthResponse login(AuthRequest request) {
        // Xác thực thông tin đăng nhập qua AuthenticationManager
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra trạng thái tài khoản
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        // Kiểm tra email đã xác thực chưa
        if (!user.getEmailVerified()) {
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        saveRefreshToken(user, refreshToken);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Starting registration process for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email {} already exists", request.getEmail());
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        log.info("Fetching default role from database...");
        Role userRole = roleRepository.findByRoleName("USER")
                .orElseGet(() -> roleRepository.findByRoleName("TREKKER")
                        .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND)));

        log.info("Encoding password for user: {}", request.getEmail());
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        log.info("Creating new User entity...");
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPasswordHash(encodedPassword);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        user.getRoles().add(userRole);

        log.info("Saving User {} to database...", request.getEmail());
        user = userRepository.save(user);
        log.info("User {} saved successfully with ID: {}", user.getEmail(), user.getUserID());

        log.info("Generating verification token...");
        String verificationToken = tokenProvider.generateVerificationToken(user.getEmail());

        // TODO: Thay thế bằng domain thực khi deploy
        String verificationUrl = "http://localhost:8080/api/v1/auth/verify?token=" + verificationToken;

        log.info("Sending verification email to: {}", user.getEmail());
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationUrl);

        log.info("Registration process completed successfully for: {}", request.getEmail());
        return RegisterResponse.builder()
                .userID(user.getUserID())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .message("User registered successfully. Please check your email to verify your account.")
                .build();
    }

    @Override
    @Transactional
    public String verifyEmail(String token) {
        log.info("Starting email verification process with token...");

        if (!tokenProvider.validateToken(token)) {
            log.error("Invalid or expired verification token.");
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        String email = tokenProvider.getEmailFromToken(token);
        log.info("Token validated for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User with email {} not found.", email);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        if (user.getEmailVerified()) {
            log.info("Email {} is already verified.", email);
            return "Email is already verified.";
        }

        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Email {} has been successfully verified.", email);

        return "Email verified successfully! You can now log in.";
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

        if (tokenEntity.getStatus() != TokenStatus.ACTIVE || tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "Refresh token đã hết hạn hoặc bị thu hồi");
        }

        User user = tokenEntity.getUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        if (!jwtService.isTokenValid(refreshTokenStr, userDetails)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        tokenEntity.setStatus(TokenStatus.EXPIRED);
        tokenEntity.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(tokenEntity);

        String accessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        saveRefreshToken(user, newRefreshToken);

        return buildAuthResponse(user, accessToken, newRefreshToken);
    }

    private void saveRefreshToken(User user, String refreshToken) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(refreshToken);
        token.setStatus(TokenStatus.ACTIVE);
        Date expiryDate = new Date(System.currentTimeMillis() + refreshExpiration);
        token.setExpiresAt(LocalDateTime.ofInstant(expiryDate.toInstant(), ZoneId.systemDefault()));
        refreshTokenRepository.save(token);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());

        UserResponse userResponse = UserResponse.builder()
                .id(user.getUserID().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .roles(roles)
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .build();
    }
}
