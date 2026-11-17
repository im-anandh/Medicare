package com.medicare.exception;

/**
 * For business-rule failures that aren't simple field validation (that's
 * what MethodArgumentNotValidException/@Valid already handles) but
 * also aren't "not found" - e.g. "you can't book an appointment in
 * the past." Still a client mistake, still a 400, just discovered by
 * our own logic instead of an annotation.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
