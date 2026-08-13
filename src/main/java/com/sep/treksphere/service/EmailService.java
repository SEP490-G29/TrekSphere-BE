package com.sep.treksphere.service;

import com.sep.treksphere.dto.email.BookingConfirmationEmailData;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);
    void sendVerificationEmail(String to, String fullName, String verificationUrl);
    void sendStaffInvitationEmail(String toEmail, String fullName, String companyName, String password, String activationUrl);
    void sendBookingConfirmationEmail(BookingConfirmationEmailData data);
}
