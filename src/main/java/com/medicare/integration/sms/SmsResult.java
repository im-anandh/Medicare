package com.medicare.integration.sms;

/**
 * The outcome of ONE attempt to send an SMS. Every provider returns
 * this same shape, so the reminder code never needs to know which
 * provider is behind the SmsService interface.
 *
 * A provider must NEVER throw for an ordinary failure (bad number,
 * rejected by the provider, network down) - it returns failure(...)
 * instead, so one bad message can't break the whole reminder run.
 *
 * (A "record" is a compact Java 16+ class: it gives us the constructor,
 * the getters success() / providerMessageId() / error(), equals and
 * toString automatically.)
 */
public record SmsResult(boolean success, String providerMessageId, String error) {

    public static SmsResult success(String providerMessageId) {
        return new SmsResult(true, providerMessageId, null);
    }

    public static SmsResult failure(String error) {
        return new SmsResult(false, null, error);
    }
}
