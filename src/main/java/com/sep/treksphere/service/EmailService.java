package com.sep.treksphere.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);
}
