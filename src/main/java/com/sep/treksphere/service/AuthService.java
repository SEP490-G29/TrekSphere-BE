package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.AuthRequest;
import com.sep.treksphere.dto.request.ChangePasswordRequest;
import com.sep.treksphere.dto.request.RegisterRequest;
import com.sep.treksphere.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(AuthRequest request);
    AuthResponse register(RegisterRequest request);
    AuthResponse refreshToken(String refreshToken);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
    void changePassword(String email, ChangePasswordRequest request);
}
