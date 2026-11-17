package com.medicare.util;

/**
 * Turns whatever is stored in patients.phone into E.164 format
 * ("+919876543210"), which is what SMS providers require.
 *
 * Handles the usual ways Indian numbers get typed:
 *   9876543210        -> +919876543210
 *   09876543210       -> +919876543210   (leading 0 dropped)
 *   +91 98765 43210   -> +919876543210   (spaces/dashes removed)
 *   919876543210      -> +919876543210
 *   0091 9876543210   -> +919876543210   (00 means "+")
 *
 * Anything that doesn't look like a real number returns null, so the
 * caller can record a clear "invalid phone number" failure instead of
 * sending garbage to a provider.
 */
public final class PhoneNumberUtil {

    private PhoneNumberUtil() {
    }

    public static String normalize(String raw, String defaultCountryCode) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().replaceAll("[\\s\\-().]", "");
        if (s.isEmpty()) {
            return null;
        }
        if (s.startsWith("00")) {
            s = "+" + s.substring(2);
        }

        // Already international: "+<country><number>"
        if (s.startsWith("+")) {
            String digits = s.substring(1);
            return digits.matches("\\d{8,15}") ? "+" + digits : null;
        }

        if (!s.matches("\\d+")) {
            return null;
        }

        String cc = (defaultCountryCode == null) ? "" : defaultCountryCode.replace("+", "").trim();

        if (cc.equals("91")) {
            // India: mobile numbers are 10 digits starting 6-9.
            if (s.startsWith("0")) {
                s = s.substring(1);
            }
            if (s.matches("[6-9]\\d{9}")) {
                return "+91" + s;
            }
            if (s.matches("91[6-9]\\d{9}")) {
                return "+" + s;
            }
            return null;
        }

        // Any other default country: accept a plausible national number.
        if (s.startsWith("0")) {
            s = s.substring(1);
        }
        if (!cc.isEmpty() && s.matches("\\d{6,12}")) {
            return "+" + cc + s;
        }
        return null;
    }
}
