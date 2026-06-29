package com.sep.treksphere.controller;

import com.sep.treksphere.dto.auth.RegisterRequest;
import com.sep.treksphere.dto.auth.RegisterResponse;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("REST request to register new user: {}", request.getEmail());
        try {
            RegisterResponse response = authService.register(request);
            log.info("REST response: Registration successful for {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.<RegisterResponse>builder()
                            .code(HttpStatus.CREATED.value())
                            .message("Registration successful")
                            .data(response)
                            .build()
            );
        } catch (Exception e) {
            log.error("REST response: Registration failed for {} - {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }
    
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam("token") String token) {
        log.info("REST request to verify email with token");
        try {
            String result = authService.verifyEmail(token);
            log.info("REST response: Email verified successfully.");
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("REST response: Email verification failed - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }
}
