package com.medicare.util;

import com.medicare.exception.InvalidRequestException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validation for bulk (array) requests.
 *
 * For a normal single-object request, putting @Valid on the controller
 * parameter makes Spring check the @NotBlank/@Pattern/... annotations on
 * the DTO. Spring does NOT do that for the items inside a plain JSON
 * array, so here we run the very same annotations ourselves, item by
 * item, with the standard jakarta.validation Validator. The rules are
 * therefore identical to the single-create endpoints - they live in one
 * place (the DTO annotations).
 *
 * Errors are collected for ALL items (not just the first bad one) and
 * keyed "[index].field", so one response lists everything to fix.
 */
@Component
public class BulkRequestValidator {

    public static final int MAX_ITEMS = 100;

    private final Validator validator;

    public BulkRequestValidator(Validator validator) {
        this.validator = validator;
    }

    // A malformed size is a request-level mistake, reported as one clear message.
    public void requireValidSize(List<?> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidRequestException("Send a JSON array with at least one item");
        }
        if (items.size() > MAX_ITEMS) {
            throw new InvalidRequestException(
                    "At most " + MAX_ITEMS + " items are allowed per request (received " + items.size() + ")");
        }
    }

    public <T> Map<String, String> validateItems(List<T> items) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            if (item == null) {
                errors.put("[" + i + "]", "Item must be an object, not null");
                continue;
            }
            // A lambda may only use variables that never change after being
            // set, and the loop counter i does change - so copy it first.
            final int index = i;
            validator.validate(item).stream()
                    .sorted(Comparator.comparing((ConstraintViolation<T> v) -> v.getPropertyPath().toString()))
                    .forEach(v -> errors.put("[" + index + "]." + v.getPropertyPath(), v.getMessage()));
        }
        return errors;
    }
}
