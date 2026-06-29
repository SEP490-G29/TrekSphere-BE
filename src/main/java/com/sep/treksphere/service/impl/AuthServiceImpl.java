package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.auth.RegisterRequest;
import com.sep.treksphere.dto.auth.RegisterResponse;
import com.sep.treksphere.entity.Role;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.user.UserStatus;
import com.sep.treksphere.repository.RoleRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.security.JwtTokenProvider;
import com.sep.treksphere.service.AuthService;
import com.sep.treksphere.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Starting registration process for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email {} already exists", request.getEmail());
            throw new RuntimeException("Email already in use!");
        }

        log.info("Fetching TREKKER role from database...");
        Role trekkerRole = roleRepository.findByRoleName("TREKKER")
                .orElseThrow(() -> {
                    log.error("Role TREKKER not found in database.");
                    return new RuntimeException("Default role not found.");
                });

        log.info("Encoding password for user: {}", request.getEmail());
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        log.info("Creating new User entity...");
        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(encodedPassword)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .roles(Collections.singleton(trekkerRole))
                .build();

        log.info("Saving User {} to database...", request.getEmail());
        userRepository.save(user);
        log.info("User {} saved successfully with ID: {}", user.getEmail(), user.getUserID());

        log.info("Generating verification token...");
        String verificationToken = tokenProvider.generateVerificationToken(user.getEmail());
        
        // TODO: Replace with actual domain when deployed
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
            throw new RuntimeException("Invalid or expired token.");
        }
        
        String email = tokenProvider.getEmailFromToken(token);
        log.info("Token validated for email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User with email {} not found.", email);
                    return new RuntimeException("User not found.");
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
}
