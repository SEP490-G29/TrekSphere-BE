package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.AuthRequest;
import com.sep.treksphere.dto.request.RegisterRequest;
import com.sep.treksphere.dto.response.AuthResponse;
import com.sep.treksphere.dto.response.RegisterResponse;

public interface AuthService {
    AuthResponse login(AuthRequest request);
    RegisterResponse register(RegisterRequest request);
    AuthResponse refreshToken(String refreshToken);
    String verifyEmail(String token);
}
