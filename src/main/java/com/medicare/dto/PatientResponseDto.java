package com.medicare.dto;

import com.medicare.entity.Gender;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * What gets sent back to the client. Notice it includes fields the
 * request DTO doesn't need to receive (id, createdAt, active) - the
 * shape of "what I send you" and "what I accept from you" are
 * naturally different, which is exactly why we use two separate DTOs
 * instead of one.
 */
public class PatientResponseDto {

    private Long id;
    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String bloodGroup;
    private String phone;
    private String email;
    private String address;
    private String emergencyContact;
    private LocalDateTime createdAt;
    private boolean active;

    public PatientResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
