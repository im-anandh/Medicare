package com.medicare.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Notice there's no @Future annotation on appointmentDate here, even
 * though "appointment cannot be in the past" is a requirement. Why?
 * @Future only looks at ONE field. If we put it on appointmentDate
 * alone, someone could book TODAY'S date with a time that already
 * passed HOURS ago, and the annotation would happily accept it,
 * because "today" itself is technically not in the past.
 *
 * The real rule needs to combine appointmentDate AND appointmentTime
 * together, compare that combined moment to right now, and only then
 * decide if it's in the past. That's a cross-field business rule, not
 * a single-field format check - so it belongs in the SERVICE layer
 * (where we have real logic), not as a validation annotation here.
 * You'll see this check in AppointmentServiceImpl.
 */
public class AppointmentRequestDto {

    @NotNull(message = "patientId is required")
    private Long patientId;

    @NotNull(message = "doctorId is required")
    private Long doctorId;

    @NotNull(message = "Appointment date is required")
    private LocalDate appointmentDate;

    @NotNull(message = "Appointment time is required")
    private LocalTime appointmentTime;

    private String reason;

    public AppointmentRequestDto() {
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
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
}
