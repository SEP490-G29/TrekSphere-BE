package com.sep.treksphere.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);
     void sendVerificationEmail(String to, String fullName, String verificationUrl);
}
