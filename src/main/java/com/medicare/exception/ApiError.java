package com.medicare.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * A consistent JSON "shape" for every error response the API returns,
 * so a client (Postman, or later, a real frontend) always knows what
 * fields to expect, no matter which endpoint or error occurred.
 */
public class ApiError {

    private LocalDateTime timestamp = LocalDateTime.now();
    private int status;
    private String error;
    private String message;
    // Only populated for validation errors - maps field name to the
    // specific problem, e.g. {"phone": "Phone number format is invalid"}
    private Map<String, String> fieldErrors;

    public ApiError(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public ApiError(int status, String error, String message, Map<String, String> fieldErrors) {
        this(status, error, message);
        this.fieldErrors = fieldErrors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
