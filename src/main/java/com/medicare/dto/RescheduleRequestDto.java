package com.medicare.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A separate, smaller DTO just for rescheduling - it only needs a new
 * date and time, not the full set of fields AppointmentRequestDto
 * requires (we're not changing which patient or doctor is involved,
 * just when). This is a common pattern: one entity can have several
 * different "input shapes" depending on which operation is being
 * performed.
 */
public class RescheduleRequestDto {

    @NotNull(message = "New appointment date is required")
    private LocalDate appointmentDate;

    @NotNull(message = "New appointment time is required")
    private LocalTime appointmentTime;

    public RescheduleRequestDto() {
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
}
