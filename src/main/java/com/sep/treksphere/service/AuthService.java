package com.sep.treksphere.service;

import com.sep.treksphere.dto.auth.RegisterRequest;
import com.sep.treksphere.dto.auth.RegisterResponse;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);
    String verifyEmail(String token);
}
