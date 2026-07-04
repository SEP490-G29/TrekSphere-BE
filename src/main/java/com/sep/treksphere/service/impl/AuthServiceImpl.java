package com.sep.treksphere.service.impl;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.ChangePasswordRequest;
import com.sep.treksphere.dto.request.LoginRequest;
import com.sep.treksphere.dto.request.RegisterRequest;
import com.sep.treksphere.dto.response.LoginResponse;
import com.sep.treksphere.dto.response.RegisterResponse;
import com.sep.treksphere.entity.RefreshToken;
import com.sep.treksphere.entity.Role;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.user.TokenStatus;
import com.sep.treksphere.enums.user.UserStatus;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.enums.user.AuthProvider;
import com.sep.treksphere.mapper.AuthMapper;
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
import java.util.Collections;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final AuthMapper authMapper;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    @Value("${application.frontend.reset-password-url}")
    private String frontendResetUrl;

    @Value("${application.security.oauth2.google.client-id}")
    private String googleClientId;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        if (!user.isEmailVerified()) {
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        saveRefreshToken(user, refreshToken);

        return authMapper.toLoginResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Starting registration process for email: {}", request.getEmail());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            log.warn("Registration failed: Passwords do not match for email {}", request.getEmail());
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Mật khẩu xác nhận không khớp");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email {} already exists", request.getEmail());
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        log.info("Fetching default role from database...");
        Role userRole = roleRepository.findByRoleName("TREKKER")
                .orElseThrow(() -> {
                    log.error("Default role 'TREKKER' not found in the database.");
                    return new AppException(ErrorCode.ROLE_NOT_FOUND);
                });

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

        String verificationUrl = "http://localhost:8080/api/v1/auth/verify?token=" + verificationToken;

        log.info("Sending verification email to: {}", user.getEmail());
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationUrl);

        log.info("Registration process completed successfully for: {}", request.getEmail());
        return RegisterResponse.builder()
                .userID(user.getUserID())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

    @Override
    @Transactional
    public String verifyEmail(String token) {
        log.info("Starting email verification process with token...");
        String message = "Xác minh email thành công! Bạn có thể đăng nhập ngay bây giờ.";
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

        if (user.isEmailVerified()) {
            log.info("Email {} is already verified.", email);
            message = "Email đã được xác minh.";
        } else {
            user.setEmailVerified(true);
            userRepository.save(user);
            log.info("Email {} has been successfully verified.", email);
        }
        return message;
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(String refreshTokenStr) {
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

        return authMapper.toLoginResponse(user, accessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String tokenStr = jwtService.generatePasswordResetToken(user);

        String resetLink = frontendResetUrl + "?token=" + tokenStr;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String email;
        try {
            email = jwtService.extractUsername(token);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!jwtService.validatePasswordResetToken(token, user)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, MessageConstant.CURRENT_PASSWORD_INCORRECT);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, MessageConstant.NEW_PASSWORD_SAME_AS_OLD);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public LoginResponse googleLogin(String idTokenParam) {
        try {
            GoogleIdTokenVerifier verifier = 
                new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenParam);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");
                String subject = payload.getSubject();

                User user = userRepository.findByEmail(email).orElse(null);
                
                if (user == null) {
                    Role userRole = roleRepository.findByRoleName("TREKKER")
                            .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
                            
                    user = new User();
                    user.setEmail(email);
                    user.setFullName(name);
                    user.setAvatarUrl(pictureUrl);
                    user.setProvider(AuthProvider.GOOGLE);
                    user.setProviderId(subject);
                    user.setStatus(UserStatus.ACTIVE);
                    user.setEmailVerified(true);
                    user.getRoles().add(userRole);
                    user = userRepository.save(user);
                } else {
                    if (user.getProviderId() == null) {
                        user.setProviderId(subject);
                        userRepository.save(user);
                    }
                }

                if (user.getStatus() != UserStatus.ACTIVE) {
                    throw new AppException(ErrorCode.USER_NOT_ACTIVE);
                }

                CustomUserDetails userDetails = new CustomUserDetails(user);
                String accessToken = jwtService.generateToken(userDetails);
                String refreshToken = jwtService.generateRefreshToken(userDetails);

                saveRefreshToken(user, refreshToken);

                return authMapper.toLoginResponse(user, accessToken, refreshToken);
            } else {
                throw new AppException(ErrorCode.INVALID_TOKEN, "Google ID token không hợp lệ.");
            }
        } catch (Exception e) {
            log.error("Google login failed", e);
            throw new AppException(ErrorCode.INVALID_TOKEN, "Google login failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenRepository.findByToken(refreshTokenStr)
                .ifPresent(tokenEntity -> {
                    tokenEntity.setStatus(TokenStatus.REVOKED);
                    tokenEntity.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(tokenEntity);
                    log.info("Refresh token revoked successfully during logout.");
                });
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


}