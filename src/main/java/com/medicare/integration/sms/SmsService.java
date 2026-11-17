package com.medicare.integration.sms;

/**
 * THE ABSTRACTION. The rest of the application only ever talks to this
 * interface. Which implementation is behind it (the console-logging
 * mock, Twilio, or a provider you add later) is decided in ONE place,
 * SmsConfig, from the sms.provider setting - so switching providers
 * needs no change to reminder logic at all.
 *
 * To add another provider (MSG91, Fast2SMS, ...): write a class that
 * implements this interface and add one case to SmsConfig.
 */
public interface SmsService {

    /**
     * @param toPhone phone number already normalised to E.164 form,
     *                e.g. "+919876543210"
     * @param message the text to send
     */
    SmsResult send(String toPhone, String message);

    /**
     * Short name of this provider ("mock", "twilio", ...). The web app uses it
     * to warn administrators when messages are only being SIMULATED, so a
     * "Sent" entry in the log is never mistaken for a real delivery.
     */
    default String providerName() {
        return "custom";
    }
}
