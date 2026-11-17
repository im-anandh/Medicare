package com.medicare.dto;

import com.medicare.entity.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * We're reusing PatientResponseDto and DoctorResponseDto here rather
 * than inventing new "summary" classes. This works cleanly because
 * neither of those DTOs references appointments back - so nesting
 * them here creates no risk of the circular reference problem we
 * discussed back in Phase 2 (Patient -> Appointments -> Patient ->
 * ...). A DTO is safe to nest as long as it doesn't eventually point
 * back to where it started.
 */
public class AppointmentResponseDto {

    private Long id;
    private PatientResponseDto patient;
    private DoctorResponseDto doctor;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String reason;
    private AppointmentStatus status;
    private LocalDateTime createdAt;
    private boolean reminderSent;

    public AppointmentResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PatientResponseDto getPatient() {
        return patient;
    }

    public void setPatient(PatientResponseDto patient) {
        this.patient = patient;
    }

    public DoctorResponseDto getDoctor() {
        return doctor;
    }

    public void setDoctor(DoctorResponseDto doctor) {
        this.doctor = doctor;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(boolean reminderSent) {
        this.reminderSent = reminderSent;
    }
}
