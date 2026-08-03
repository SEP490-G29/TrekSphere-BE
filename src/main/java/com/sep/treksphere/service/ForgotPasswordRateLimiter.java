package com.sep.treksphere.service;

public interface ForgotPasswordRateLimiter {
    void checkAllowed(String email);
}
