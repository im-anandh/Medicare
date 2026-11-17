package com.medicare.config;

import com.medicare.integration.sms.MockSmsService;
import com.medicare.integration.sms.PhoneGatewaySmsService;
import com.medicare.integration.sms.SmsService;
import com.medicare.integration.sms.TwilioSmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chooses the SmsService from sms.provider:
 *   mock    - logs only, sends nothing (default)
 *   gateway - REAL SMS from your own Android phone via textbee.dev
 *   twilio  - real SMS through Twilio
 */
@Configuration
public class SmsConfig {

    private static final Logger log = LoggerFactory.getLogger(SmsConfig.class);

    @Bean
    public SmsService smsService(
            @Value("${sms.provider:mock}") String provider,
            @Value("${sms.mock.simulate-failure:false}") boolean mockSimulateFailure,
            @Value("${sms.twilio.account-sid:}") String twilioSid,
            @Value("${sms.twilio.auth-token:}") String twilioToken,
            @Value("${sms.twilio.from-number:}") String twilioFrom,
            @Value("${sms.gateway.api-key:}") String gatewayKey,
            @Value("${sms.gateway.base-url:}") String gatewayUrl) {

        String choice = provider == null ? "" : provider.trim().toLowerCase();
        switch (choice) {
            case "mock":
                log.info("SMS provider: MOCK (messages are only logged, nothing is sent)");
                return new MockSmsService(mockSimulateFailure);
            case "gateway":
                log.info("SMS provider: PHONE GATEWAY (real SMS sent from your Android phone's SIM)");
                return new PhoneGatewaySmsService(gatewayKey, gatewayUrl);
            case "twilio":
                log.info("SMS provider: TWILIO (real messages will be sent)");
                return new TwilioSmsService(twilioSid, twilioToken, twilioFrom);
            default:
                throw new IllegalStateException(
                        "Unknown sms.provider '" + provider + "'. Valid values: mock, gateway, twilio");
        }
    }
}
