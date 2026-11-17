package com.medicare.exception;

import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

/**
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody. It means
 * "this class handles exceptions thrown by ANY controller in the
 * application, globally, in one place" - instead of every controller
 * method needing its own try/catch blocks.
 *
 * Each @ExceptionHandler method below says "when THIS type of
 * exception is thrown anywhere during request handling, run this
 * method instead of letting the error crash out as a raw stack
 * trace." This is exactly what satisfies the spec's requirement:
 * "Do not expose stack traces or sensitive internal information to
 * API users."
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Triggered by our own ResourceNotFoundException, e.g. thrown when
    // getPatientById() can't find a matching row.
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        ApiError error = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // Business-rule failures discovered in the service layer, e.g.
    // "appointment date cannot be in the past."
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(InvalidRequestException ex) {
        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_REQUEST",
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Double-booking attempts - deliberately a distinct 409, not 400,
    // per the reasoning in AppointmentConflictException's own comments.
    @ExceptionHandler(AppointmentConflictException.class)
    public ResponseEntity<ApiError> handleAppointmentConflict(AppointmentConflictException ex) {
        ApiError error = new ApiError(
                HttpStatus.CONFLICT.value(),
                "APPOINTMENT_CONFLICT",
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // Triggered automatically by Spring when @Valid validation (on a
    // request DTO like PatientRequestDto) fails. We loop through every
    // failed field and build a clear map of "field -> what went wrong"
    // instead of Spring's default, harder-to-read error format.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage())
        );
        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "One or more fields are invalid",
                fieldErrors
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Triggered when the DATABASE itself rejects an insert/update
    // because it violates a constraint we defined back in Phase 2 -
    // most commonly a @Column(unique = true) field (email, phone,
    // username) that already exists on another row. This is a
    // "conflict with existing data" situation, same category as our
    // AppointmentConflictException, so we map it to the same 409
    // status rather than letting it fall through to a raw 500.
    //
    // We deliberately DON'T try to parse out exactly which field/value
    // caused it from the raw database error message - that message
    // format is database-specific and brittle to depend on. A clear,
    // generic message is more reliable than a fragile "clever" one.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logger.warn("Data integrity violation: {}", ex.getMessage());
        ApiError error = new ApiError(
                HttpStatus.CONFLICT.value(),
                "DUPLICATE_VALUE",
                "A record with one of these values (e.g. email, phone, or username) already exists"
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // Triggered when our OWN service-layer code detects an ownership
    // violation - e.g. a PATIENT requesting another patient's record,
    // or a DOCTOR requesting a patient they've never had an
    // appointment with. NOTE: this is a DIFFERENT scenario from a
    // role check failing at the SecurityConfig gate (that's handled
    // by RestAccessDeniedHandler instead, since it happens earlier,
    // in the security filter chain, before a request ever reaches a
    // controller). This handler only catches AccessDeniedException
    // thrown manually, from inside our own service code - which DOES
    // pass through normal Spring MVC exception handling.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        ApiError error = new ApiError(
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // The request body could not even be read: broken JSON syntax, an
    // empty body, or a value of the wrong type - most commonly a
    // misspelled enum such as "status": "PAIDD". That is the client's
    // mistake, so it must be a 400, not the generic 500 below. (The
    // details are logged at WARN so you can still see the cause in the
    // terminal while developing.)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        logger.warn("Unreadable request body: {}", ex.getMessage());
        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "MALFORMED_REQUEST",
                "Request body is missing, is not valid JSON, or contains an invalid value "
                        + "(for example a misspelled status or role)"
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // One or more items of a bulk (array) request were invalid. Same
    // response shape as normal validation errors, but the field names are
    // item-indexed, e.g. "[2].phone". Nothing was saved.
    @ExceptionHandler(BulkValidationException.class)
    public ResponseEntity<ApiError> handleBulkValidation(BulkValidationException ex) {
        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                ex.getMessage(),
                ex.getFieldErrors()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // The framework's own "your request is wrong" exceptions - calling a
    // URL that doesn't exist (404), using the wrong HTTP method on a real
    // URL (405), sending the body in the wrong Content-Type (415), a
    // missing request parameter (400), and so on. They all extend
    // ServletException and carry their correct HTTP status.
    //
    // Without this handler they fell into the catch-all below and came
    // back as a misleading 500 "Something went wrong" - as if the SERVER
    // were broken when it was really the request that was wrong.
    @ExceptionHandler(ServletException.class)
    public ResponseEntity<ApiError> handleFrameworkRequestError(ServletException ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode statusCode = errorResponse.getStatusCode();
            if (statusCode.is4xxClientError()) {
                int status = statusCode.value();
                String code;
                String message;
                switch (status) {
                    case 404 -> {
                        code = "NOT_FOUND";
                        message = "No endpoint exists at this URL";
                    }
                    case 405 -> {
                        code = "METHOD_NOT_ALLOWED";
                        message = "This HTTP method is not supported for this URL";
                    }
                    case 415 -> {
                        code = "UNSUPPORTED_MEDIA_TYPE";
                        message = "Unsupported content type - send the body as raw JSON "
                                + "(Content-Type: application/json)";
                    }
                    default -> {
                        code = "BAD_REQUEST";
                        message = ex.getMessage();
                    }
                }
                return new ResponseEntity<>(new ApiError(status, code, message), statusCode);
            }
        }
        // Anything else really is unexpected - keep the old behaviour.
        return handleGeneric(ex);
    }

    // A URL or query value of the wrong type, e.g. GET /api/patients/abc
    // (the id must be a number) or ?status=NOPE for an enum.
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(TypeMismatchException ex) {
        String value = String.valueOf(ex.getValue());
        if (value.length() > 50) {
            value = value.substring(0, 50) + "...";
        }
        Class<?> required = ex.getRequiredType();
        String expected = (required == null) ? "a different type"
                : Number.class.isAssignableFrom(required) ? "a number" : required.getSimpleName();
        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_REQUEST",
                "Invalid value '" + value + "' in the URL or request parameters (expected " + expected + ")"
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // A catch-all safety net. Any exception we didn't anticipate still
    // returns a clean generic 500 response instead of leaking a raw
    // Java stack trace to the client.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        logger.error("Unhandled exception occurred", ex);
        ApiError error = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "Something went wrong. Please try again later."
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
