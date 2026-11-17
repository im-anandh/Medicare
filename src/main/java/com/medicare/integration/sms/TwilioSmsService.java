package com.medicare.integration.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Real provider: Twilio's REST API, called with the HTTP client that is
 * built into Java 11+ - so this needs NO extra library in pom.xml.
 *
 * The call is: POST https://api.twilio.com/2010-04-01/Accounts/{SID}/Messages.json
 * with HTTP Basic auth (account SID : auth token) and a form-encoded
 * body of To, From and Body. A 2xx response contains the new message's
 * "sid"; an error response contains a "message" explaining what went
 * wrong.
 *
 * IMPORTANT (real-world limits, not code issues):
 *  - A Twilio trial account normally can only message numbers you have
 *    verified in the Twilio console.
 *  - Messaging Indian numbers is subject to India's DLT sender/template
 *    registration rules, which can block or filter delivery. Check
 *    Twilio's current India guidelines before relying on this for real.
 */
public class TwilioSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsService.class);
    private static final String DEFAULT_BASE_URL = "https://api.twilio.com/2010-04-01";

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public TwilioSmsService(String accountSid, String authToken, String fromNumber) {
        this(accountSid, authToken, fromNumber, DEFAULT_BASE_URL);
    }

    // The base URL is overridable only so the class can be tested
    // against a local fake server instead of the real Twilio.
    public TwilioSmsService(String accountSid, String authToken, String fromNumber, String baseUrl) {
        if (isBlank(accountSid) || isBlank(authToken) || isBlank(fromNumber)) {
            // Fail FAST at application startup with a clear message,
            // rather than failing later on the first reminder.
            throw new IllegalStateException(
                    "sms.provider=twilio requires TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN "
                            + "and TWILIO_FROM_NUMBER to be set");
        }
        this.accountSid = accountSid.trim();
        this.authToken = authToken.trim();
        this.fromNumber = fromNumber.trim();
        this.baseUrl = baseUrl;
    }

    @Override
    public String providerName() {
        return "twilio";
    }

    @Override
    public SmsResult send(String toPhone, String message) {
        String form = "To=" + enc(toPhone) + "&From=" + enc(fromNumber) + "&Body=" + enc(message);
        String credentials = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/Accounts/" + accountSid + "/Messages.json"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body() == null ? "" : response.body();

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String sid = jsonString(body, "sid");
                log.info("Twilio accepted SMS to {} (sid {})", toPhone, sid);
                return SmsResult.success(sid);
            }

            String detail = jsonString(body, "message");
            String code = jsonNumber(body, "code");
            String error = "Twilio HTTP " + response.statusCode()
                    + (code != null ? " (code " + code + ")" : "")
                    + (detail != null ? ": " + detail : "");
            log.warn("Twilio rejected SMS to {}: {}", toPhone, error);
            return SmsResult.failure(error);

        } catch (IOException e) {
            // Some network exceptions carry no message, so fall back to
            // the exception type to avoid a useless "...: null" in the log.
            String reason = (e.getMessage() != null && !e.getMessage().isBlank())
                    ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("Network error calling Twilio: {}", reason);
            return SmsResult.failure("Network error calling Twilio: " + reason);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SmsResult.failure("Interrupted while calling Twilio");
        }
    }

    // ---- tiny helpers (Twilio's replies are flat JSON, so a regex is
    // enough and avoids depending on a JSON library here) ----

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    static String jsonString(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        return m.find() ? m.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : null;
    }

    static String jsonNumber(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
