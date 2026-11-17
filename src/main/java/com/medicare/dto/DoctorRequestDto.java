package com.medicare.dto;

import jakarta.validation.constraints.*;

/**
 * Notice departmentId is a plain Long here, not a Department object.
 * A client creating a doctor doesn't send a whole department JSON
 * blob - it sends the ID of an EXISTING department, and the service
 * layer looks that department up from the database. This is the
 * standard way to represent "pick an existing related record" in a
 * REST API request.
 */
public class DoctorRequestDto {

    @NotBlank(message = "Doctor name is required")
    private String name;

    @NotBlank(message = "Specialization is required")
    private String specialization;

    @NotNull(message = "departmentId is required")
    private Long departmentId;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+\\-() ]{7,15}$", message = "Phone number format is invalid")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotNull(message = "Experience is required")
    @Min(value = 0, message = "Experience cannot be negative")
    @Max(value = 70, message = "Experience value seems unrealistic")
    private Integer experience;

    private String availability;

    public DoctorRequestDto() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
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

    public Integer getExperience() {
        return experience;
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }
}
