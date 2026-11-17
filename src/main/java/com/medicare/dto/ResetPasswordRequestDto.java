package com.medicare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of PATCH /api/users/{id}/password. */
public class ResetPasswordRequestDto {

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
    private String newPassword;

    public ResetPasswordRequestDto() {
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
