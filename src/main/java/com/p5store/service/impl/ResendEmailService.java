package com.p5store.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p5store.exception.BusinessException;
import com.p5store.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResendEmailService implements EmailService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper;

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from-email:}")
    private String fromEmail;

    private record ResendEmailRequest(String from, List<String> to, String subject, String html) {}

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        if (apiKey.isBlank() || fromEmail.isBlank()) {
            throw new BusinessException("Email sending is not configured");
        }

        String html = "<p>We received a request to reset your Pillar 5 account password.</p>"
                + "<p><a href=\"" + resetLink + "\">Click here to reset your password</a></p>"
                + "<p>This link expires in 1 hour. If you didn't request this, you can safely ignore this email.</p>";

        ResendEmailRequest payload = new ResendEmailRequest(
                fromEmail, List.of(toEmail), "Reset your Pillar 5 password", html);

        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.error("Resend API error {}: {}", response.statusCode(), response.body());
                throw new BusinessException("Failed to send reset email");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Failed to send reset email");
        }
    }
}
