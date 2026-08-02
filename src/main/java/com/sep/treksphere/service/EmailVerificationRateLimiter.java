package com.sep.treksphere.service;

public interface EmailVerificationRateLimiter {
    void checkAllowed(String email);
}
