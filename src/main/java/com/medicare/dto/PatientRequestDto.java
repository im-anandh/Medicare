package com.medicare.dto;

import com.medicare.entity.Gender;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * WHY DTOs INSTEAD OF RETURNING/ACCEPTING THE ENTITY DIRECTLY?
 *
 * 1. Control over the API shape. Our Patient entity has a "user" field
 *    (the linked login account) - we don't want that exposed in JSON
 *    responses, and we definitely don't want a client able to set it
 *    directly through a request body. A DTO only contains the fields
 *    we explicitly decide to expose.
 *
 * 2. Validation belongs on input, not on the database model. The
 *    entity describes what's valid to STORE; the DTO (this class)
 *    describes what's valid to RECEIVE from a client, which can be
 *    stricter (e.g. requiring a phone number format on input even if
 *    the column itself would technically accept any string).
 *
 * 3. Decoupling. If we redesign the Patient entity later (e.g. split
 *    "address" into street/city/zip), we can keep this DTO's shape
 *    stable for existing frontend/Postman clients, or version it
 *    deliberately - instead of every entity change automatically
 *    breaking the API.
 *
 * VALIDATION ANNOTATIONS EXPLAINED:
 *   @NotBlank      - string must not be null AND not just whitespace
 *   @NotNull       - value must not be null (used for non-string types)
 *   @Past          - date must be before today (you can't be born tomorrow)
 *   @Email         - must look like a valid email address
 *   @Pattern        - must match a given regular expression
 *
 * These run automatically when the controller method parameter is
 * annotated with @Valid - Spring checks every rule here BEFORE the
 * method body even runs, and returns a 400 Bad Request automatically
 * if anything fails (we'll wire up exactly how that response looks
 * in our GlobalExceptionHandler).
 */
public class PatientRequestDto {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    private String bloodGroup;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+\\-() ]{7,15}$", message = "Phone number format is invalid")
    private String phone;

    @Email(message = "Email must be a valid email address")
    private String email;

    private String address;

    private String emergencyContact;

    public PatientRequestDto() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }
}
