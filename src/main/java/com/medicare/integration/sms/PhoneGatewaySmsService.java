package com.medicare.integration.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Real SMS with no DLT registration and no verified-number limit: your
 * own Android phone (with its normal SIM) is the sender, controlled by
 * the textbee.dev gateway app.
 *
 * Call: POST {baseUrl}/gateway/send-sms
 *       header x-api-key, JSON body {"recipients":["+91..."],"message":"..."}
 * Uses the HTTP client built into Java, so no extra library is needed.
 */
public class PhoneGatewaySmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(PhoneGatewaySmsService.class);

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public PhoneGatewaySmsService(String apiKey, String baseUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("sms.provider=gateway requires SMS_GATEWAY_API_KEY to be set "
                    + "(create it at textbee.dev after linking your Android phone)");
        }
        this.apiKey = apiKey.trim();
        String b = (baseUrl == null || baseUrl.isBlank()) ? "https://api.textbee.dev/api/v1" : baseUrl.trim();
        this.baseUrl = b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
    }

    @Override
    public SmsResult send(String toPhone, String message) {
        String body = "{\"recipients\":[\"" + esc(toPhone) + "\"],\"message\":\"" + esc(message) + "\"}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/gateway/send-sms"))
                .timeout(Duration.ofSeconds(20))
                .header("x-api-key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            String text = res.body() == null ? "" : res.body();
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                log.info("Phone gateway accepted SMS to {}", toPhone);
                return SmsResult.success("gw-" + System.currentTimeMillis());
            }
            String shortText = text.length() > 150 ? text.substring(0, 150) : text;
            log.warn("Phone gateway rejected SMS to {}: HTTP {} {}", toPhone, res.statusCode(), shortText);
            return SmsResult.failure("Gateway HTTP " + res.statusCode() + ": " + shortText);
        } catch (IOException e) {
            String r = (e.getMessage() != null && !e.getMessage().isBlank()) ? e.getMessage() : e.getClass().getSimpleName();
            return SmsResult.failure("Network error calling gateway: " + r);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SmsResult.failure("Interrupted while calling gateway");
        }
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
