package com.medicare.exception;

/**
 * Specifically for "this doctor already has an appointment at this
 * exact date and time." We give this its own exception type (rather
 * than reusing InvalidRequestException) because it maps to a
 * DIFFERENT, more precise HTTP status: 409 CONFLICT, not 400 BAD
 * REQUEST. 409 specifically means "your request is well-formed, but
 * it conflicts with the current state of the server's data" - which
 * is exactly what a double-booking is.
 */
public class AppointmentConflictException extends RuntimeException {

    public AppointmentConflictException(String message) {
        super(message);
    }
}
