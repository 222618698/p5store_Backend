package com.p5store.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.p5store.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Thin wrapper around PayPal's REST Orders v2 API (create + capture), using
 * the client-credentials OAuth flow. No PayPal SDK dependency — this is a
 * handful of plain HTTP calls, consistent with how CatalogImportService
 * talks to R2.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayPalService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper;

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    @Value("${paypal.api-base}")
    private String apiBase;

    @Value("${paypal.currency}")
    private String currency;

    @Value("${paypal.zar-to-usd-rate}")
    private BigDecimal zarToUsdRate;

    /** Creates a PayPal order for the given ZAR amount and returns its "approve" link. */
    public PayPalOrder createOrder(String orderNumber, BigDecimal totalZar, String returnUrl, String cancelUrl) {
        requireConfigured();
        String accessToken = fetchAccessToken();

        BigDecimal amountUsd = totalZar.multiply(zarToUsdRate).setScale(2, RoundingMode.HALF_UP);
        String body = """
                {
                  "intent": "CAPTURE",
                  "purchase_units": [{
                    "reference_id": "%s",
                    "custom_id": "%s",
                    "amount": { "currency_code": "%s", "value": "%s" }
                  }],
                  "application_context": {
                    "return_url": "%s",
                    "cancel_url": "%s",
                    "user_action": "PAY_NOW"
                  }
                }
                """.formatted(orderNumber, orderNumber, currency, amountUsd, returnUrl, cancelUrl);

        JsonNode response = send("/v2/checkout/orders", body, accessToken);
        String paypalOrderId = response.path("id").asText(null);
        if (paypalOrderId == null) {
            throw new BusinessException("PayPal did not return an order id");
        }
        String approvalUrl = null;
        for (JsonNode link : response.path("links")) {
            if ("approve".equals(link.path("rel").asText())) {
                approvalUrl = link.path("href").asText(null);
            }
        }
        if (approvalUrl == null) {
            throw new BusinessException("PayPal did not return an approval link");
        }
        return new PayPalOrder(paypalOrderId, approvalUrl);
    }

    /** Captures a previously-approved PayPal order. Returns the capture (transaction) id. */
    public String captureOrder(String paypalOrderId) {
        requireConfigured();
        String accessToken = fetchAccessToken();

        JsonNode response = send("/v2/checkout/orders/" + paypalOrderId + "/capture", "{}", accessToken);
        String status = response.path("status").asText();
        if (!"COMPLETED".equals(status)) {
            log.error("PayPal capture did not complete, status={}, response={}", status, response);
            throw new BusinessException("PayPal payment was not completed (status: " + status + ")");
        }
        String captureId = response
                .path("purchase_units").path(0)
                .path("payments").path("captures").path(0)
                .path("id").asText(null);
        if (captureId == null) {
            throw new BusinessException("PayPal capture succeeded but returned no capture id");
        }
        return captureId;
    }

    private String fetchAccessToken() {
        try {
            String credentials = Base64.getEncoder().encodeToString(
                    (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase + "/v1/oauth2/token"))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.error("PayPal OAuth token request failed: HTTP {} - {}", response.statusCode(), response.body());
                throw new BusinessException("Could not authenticate with PayPal");
            }
            return objectMapper.readTree(response.body()).path("access_token").asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Could not authenticate with PayPal: " + e.getMessage());
        }
    }

    private JsonNode send(String path, String jsonBody, String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase + path))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode parsed = objectMapper.readTree(response.body());
            if (response.statusCode() >= 300) {
                log.error("PayPal API call to {} failed: HTTP {} - {}", path, response.statusCode(), response.body());
                throw new BusinessException("PayPal request failed (HTTP " + response.statusCode() + ")");
            }
            return parsed;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("PayPal request failed: " + e.getMessage());
        }
    }

    private void requireConfigured() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new BusinessException("PayPal is not configured on this server");
        }
    }

    public record PayPalOrder(String paypalOrderId, String approvalUrl) {}
}
