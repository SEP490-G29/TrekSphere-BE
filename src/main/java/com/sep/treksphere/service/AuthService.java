package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.ChangePasswordRequest;
import com.sep.treksphere.dto.request.LoginRequest;
import com.sep.treksphere.dto.request.RegisterRequest;
import com.sep.treksphere.dto.response.LoginResponse;
import com.sep.treksphere.dto.response.RegisterResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    RegisterResponse register(RegisterRequest request);
    LoginResponse refreshToken(String refreshToken);
    String verifyEmail(String token);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
    void changePassword(String email, ChangePasswordRequest request);
    LoginResponse googleLogin(String idToken);
}
