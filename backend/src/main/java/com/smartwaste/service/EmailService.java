package com.smartwaste.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // OTP email
    public void sendOtp(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("SmartWaste - OTP Verification");
        message.setText(
            "Hello!\n\n" +
            "Your OTP is: " + otp + "\n\n" +
            "Valid for 5 minutes only.\n\n" +
            "If you didn't request this, ignore this email."
        );
        mailSender.send(message);
    }

    // Password reset email — accepts a full URL link
    public void sendPasswordResetLink(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("SmartWaste - Password Reset Request");
        message.setText(
            "Hello!\n\n" +
            "Click the link below to reset your password:\n\n" +
            resetLink + "\n\n" +
            "This link is valid for 15 minutes only.\n\n" +
            "If you didn't request this, ignore this email.\n\n" +
            "Team SmartWaste"
        );
        mailSender.send(message);
    }

    // Password reset email — accepts a raw token; builds the reset URL internally
    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = "http://localhost:3000/reset-password?token=" + token;
        sendPasswordResetLink(toEmail, resetLink);
    }
}