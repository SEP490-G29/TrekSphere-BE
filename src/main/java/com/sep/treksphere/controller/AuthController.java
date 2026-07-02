package com.sep.treksphere.controller;

import com.sep.treksphere.dto.request.AuthRequest;
import com.sep.treksphere.dto.request.RegisterRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.AuthResponse;
import com.sep.treksphere.dto.response.RegisterResponse;
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, authService.login(request)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("REST request to register new user: {}", request.getEmail());
        RegisterResponse response = authService.register(request);
        log.info("REST response: Registration successful for {}", request.getEmail());
        HttpStatus status = HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .body(ApiResponse.success(status, response,
                        "Đăng ký thành công. Vui lòng kiểm tra email để xác nhận tài khoản của bạn."));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam("token") String token) {
        log.info("REST request to verify email with token");
        String result = authService.verifyEmail(token);
        log.info("REST response: Email verified successfully.");
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));

    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestParam("token") String refreshToken
    ) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, authService.refreshToken(refreshToken)));
    }
}
