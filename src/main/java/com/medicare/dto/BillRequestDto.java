package com.medicare.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body of POST /api/bills: everything in BillUpdateRequestDto (charges
 * and items, inherited) plus WHO is being billed and, optionally, for
 * which appointment. Validation annotations on the parent class's
 * fields still apply to this subclass.
 *
 * The patient and appointment are fixed once the bill exists, which is
 * why they are NOT in the update DTO.
 */
public class BillRequestDto extends BillUpdateRequestDto {

    @NotNull(message = "patientId is required")
    private Long patientId;

    // Optional: a bill can also be for something not tied to an
    // appointment (e.g. a walk-in lab test).
    private Long appointmentId;

    public BillRequestDto() {
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }
}
