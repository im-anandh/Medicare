package com.medicare.dto;

import java.time.LocalDate;

public class MedicalRecordResponseDto {

    private Long id;
    private PatientResponseDto patient;
    private DoctorResponseDto doctor;
    private LocalDate visitDate;
    private String symptoms;
    private String diagnosis;
    private String treatment;
    private String medicalNotes;
    private LocalDate followUpDate;

    public MedicalRecordResponseDto() {
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

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getTreatment() {
        return treatment;
    }

    public void setTreatment(String treatment) {
        this.treatment = treatment;
    }

    public String getMedicalNotes() {
        return medicalNotes;
    }

    public void setMedicalNotes(String medicalNotes) {
        this.medicalNotes = medicalNotes;
    }

    public LocalDate getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDate followUpDate) {
        this.followUpDate = followUpDate;
    }
}
