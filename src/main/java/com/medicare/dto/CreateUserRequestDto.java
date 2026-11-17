package com.medicare.dto;

import com.medicare.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/users - an administrator (or, for patients only, a
 * receptionist) creating a login on someone's behalf. Because a member
 * of staff is vouching for the person, no identity proof is needed here
 * (unlike public patient sign-up).
 *
 * profileId is the id of the existing Patient or Doctor record the login
 * belongs to; required for PATIENT and DOCTOR, ignored for ADMIN and
 * RECEPTIONIST.
 */
public class CreateUserRequestDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    // max 72: BCrypt only looks at the first 72 bytes of a password.
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotNull(message = "Role is required")
    private Role role;

    private Long profileId;

    public CreateUserRequestDto() {
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
}
