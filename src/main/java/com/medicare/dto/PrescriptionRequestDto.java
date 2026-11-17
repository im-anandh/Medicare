package com.medicare.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Body of POST /api/prescriptions.
 *
 * Notice there is NO patientId and NO doctorId here, on purpose:
 *  - the patient is taken from the medical record being prescribed
 *    against, so a doctor can never attach a prescription to the wrong
 *    patient by typing a wrong id;
 *  - the doctor is always the logged-in doctor, so nobody can write a
 *    prescription "as" another doctor.
 */
public class PrescriptionRequestDto {

    @NotNull(message = "medicalRecordId is required")
    private Long medicalRecordId;

    // @NotEmpty: a prescription with zero medicines is meaningless.
    //
    // @Valid on the list: by default Jakarta Validation only checks the
    // annotations on THIS class. @Valid tells it to also step into each
    // PrescriptionItemRequestDto in the list and check ITS annotations
    // (@NotBlank etc.). Without @Valid, a blank medicine name inside
    // the list would sail straight through.
    @NotEmpty(message = "At least one medicine is required")
    @Valid
    private List<PrescriptionItemRequestDto> items;

    public PrescriptionRequestDto() {
    }

    public Long getMedicalRecordId() {
        return medicalRecordId;
    }

    public void setMedicalRecordId(Long medicalRecordId) {
        this.medicalRecordId = medicalRecordId;
    }

    public List<PrescriptionItemRequestDto> getItems() {
        return items;
    }

    public void setItems(List<PrescriptionItemRequestDto> items) {
        this.items = items;
    }
}
