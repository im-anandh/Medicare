package com.medicare.exception;

/**
 * A custom, meaningful exception instead of throwing a generic
 * RuntimeException everywhere. When the service layer can't find a
 * patient/doctor/appointment by id, it throws this. Our
 * GlobalExceptionHandler (below) catches it specifically and
 * converts it into a clean 404 Not Found JSON response - the
 * controller and service code never has to think about HTTP status
 * codes directly.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
