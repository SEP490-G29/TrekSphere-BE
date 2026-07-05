package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.*;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.LoginResponse;
import com.sep.treksphere.dto.response.RegisterResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Các API liên quan đến xác thực và phân quyền người dùng")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Đăng nhập", description = "Đăng nhập với email và passoword")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Attempting to log in user with email: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,authService.login(request),MessageConstant.LOGIN_SUCCESSFULLY));
    }

    @Operation(summary = "Đăng nhập bằng Google", description = "Đăng nhập thông qua Google ID Token")
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(@RequestParam("idToken") String idToken) {
        log.info("Attempting to log in user with Google ID Token");
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, authService.googleLogin(idToken), MessageConstant.GOOGLE_LOGIN_SUCCESSFULLY));
    }

    @Operation(summary = "Đăng ký tài khoản", description = "Đăng ký một tài khoản mới với quyền TREKKER")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());
        HttpStatus status = HttpStatus.CREATED;
        return ResponseEntity.status(status).body(ApiResponse.success(status, authService.register(request), MessageConstant.REGISTER_SUCCESSFULLY));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam("token") String token) {
        log.info("Attempting to verify email with token: {}", token);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, authService.verifyEmail(token)));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @RequestParam("token") String refreshToken
    ) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, authService.refreshToken(refreshToken)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.RESET_LINK_SENT_SUCCESSFULLY));
    }

    @Operation(summary = "Reset mật khẩu", description = "Cho phép người dùng reset mật khẩu bằng token")   
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.PASSWORD_RESET_SUCCESSFULLY));
    }

    @Operation(summary = "Thay đổi mật khẩu", description = "Cho phép người dùng đã đăng nhập thay đổi mật khẩu")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        

        
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.PASSWORD_CHANGED_SUCCESSFULLY));
    }

    @Operation(summary = "Đăng xuất", description = "Thu hồi refresh token của phiên bản đăng nhập hiện tại")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request, @RequestParam("token") String refreshToken) {
        String authHeader = request.getHeader("Authorization");
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, MessageConstant.LOGOUT_SUCCESSFULLY));
    }
}
