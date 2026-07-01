package com.sep.treksphere.controller;

import com.sep.treksphere.dto.request.AuthRequest;
import com.sep.treksphere.dto.request.ForgotPasswordRequest;
import com.sep.treksphere.dto.request.RegisterRequest;
import com.sep.treksphere.dto.request.ResetPasswordRequest;
import com.sep.treksphere.dto.response.AuthResponse;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Các API liên quan đến xác thực và phân quyền người dùng")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Đăng nhập", description = "Trả về JWT token nếu đăng nhập thành công")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @Operation(summary = "Đăng ký tài khoản", description = "Đăng ký một tài khoản mới với quyền TREKKER. Mật khẩu phải có ít nhất 1 chữ, 1 số và độ dài tối thiểu 8 ký tự.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.register(request)));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestParam("token") String refreshToken
    ) {
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(refreshToken)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("If the email exists, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully."));
    }
}
