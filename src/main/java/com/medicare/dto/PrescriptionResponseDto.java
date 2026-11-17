package com.medicare.dto;

import java.time.LocalDate;
import java.util.List;

public class PrescriptionResponseDto {

    private Long id;
    private LocalDate createdDate;
    private Long medicalRecordId;
    private PatientResponseDto patient;
    private DoctorResponseDto doctor;
    private List<PrescriptionItemResponseDto> items;

    public PrescriptionResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public Long getMedicalRecordId() {
        return medicalRecordId;
    }

    public void setMedicalRecordId(Long medicalRecordId) {
        this.medicalRecordId = medicalRecordId;
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

    public List<PrescriptionItemResponseDto> getItems() {
        return items;
    }

    public void setItems(List<PrescriptionItemResponseDto> items) {
        this.items = items;
    }
}
