package com.sep.treksphere.service;

public interface RefreshTokenService {
    void store(String userEmail, String jti, long ttlMillis);
    void validateAndConsume(String userEmail, String jti);
    void revokeAll(String userEmail);
}
