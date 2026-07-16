package com.p5store.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);
}
