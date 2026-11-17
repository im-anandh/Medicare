package com.medicare.entity;

/**
 * The four roles supported by the system.
 *
 * Why an enum instead of a plain String like "ADMIN"?
 *   - Typos are caught at compile time (Role.ADNIM would not compile,
 *     but a String "ADNIM" would silently exist until runtime).
 *   - We get autocomplete in the IDE.
 *   - It documents, in one place, every valid value that can ever exist.
 *
 * This will be used on the User entity, and later by Spring Security
 * to decide what each logged-in user is allowed to do.
 */
public enum Role {
    ADMIN,
    DOCTOR,
    RECEPTIONIST,
    PATIENT
}
