package com.medicare.dto;

import com.medicare.entity.Role;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RegisterRequestDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    // We only ever validate the RAW password here, at the API
    // boundary, before it gets hashed. The entity itself just stores
    // whatever hash the service produces - it never sees or cares
    // about "plain text password rules."
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotNull(message = "Role is required")
    private Role role;

    // REQUIRED when role is PATIENT or DOCTOR, ignored otherwise.
    // This must be the id of an EXISTING Patient or Doctor profile
    // (created beforehand by an admin/receptionist via POST
    // /api/patients or POST /api/doctors). Registering a login and
    // creating a medical profile are deliberately kept as two
    // separate steps - a login proves "who can access the system,"
    // while a Patient/Doctor row is the actual clinical/business
    // record. Linking them explicitly by id (rather than
    // auto-creating a profile from the registration form) means
    // there's always a clear, deliberate link between the two,
    // matching how you said you want new staff/patients onboarded.
    private Long profileId;

    // PUBLIC patient sign-up only: proof of identity. Must match the phone
    // number and date of birth already on the patient's record, so that
    // nobody can claim someone else's patient profile just by guessing its
    // id. (Accounts created by an administrator don't use this DTO.)
    private String phone;

    private LocalDate dateOfBirth;

    public RegisterRequestDto() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}
