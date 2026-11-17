package com.medicare.exception;

import java.util.Map;

/**
 * Thrown when one or more items in a bulk request are invalid.
 * fieldErrors maps "[index].field" to a message, e.g.
 *   "[2].phone"        -> "Phone number format is invalid"
 *   "[4].departmentId" -> "Department not found with id: 99"
 * so the caller can fix every problem in one go. A bulk request is
 * all-or-nothing: if this is thrown, NOTHING has been saved.
 */
public class BulkValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public BulkValidationException(Map<String, String> fieldErrors) {
        super(fieldErrors.size() + " problem(s) found in the request - nothing was saved");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
