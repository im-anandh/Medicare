package com.medicare.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A Prescription is the "header" - it identifies which patient, doctor,
 * and medical record it belongs to. The actual medicines are stored as
 * separate PrescriptionItem rows, because one prescription can include
 * multiple medicines (e.g. Paracetamol AND Amoxicillin in one visit).
 *
 * This is our first @OneToMany - the reverse side of PrescriptionItem's
 * @ManyToOne. A few new things to understand here:
 *
 * mappedBy = "prescription"
 *   Tells Hibernate "the foreign key for this relationship is already
 *   defined over on the PrescriptionItem class, on its 'prescription'
 *   field - don't create a second, duplicate foreign key column here."
 *   Exactly one side of a bidirectional relationship owns the foreign
 *   key; the other side just declares mappedBy and points to it.
 *
 * cascade = CascadeType.ALL
 *   When we save, update, or delete a Prescription, automatically do
 *   the same to all of its PrescriptionItems. This makes sense here
 *   because a PrescriptionItem has no meaning on its own - it only
 *   exists as part of a specific Prescription.
 *
 * orphanRemoval = true
 *   If we remove an item from the "items" list in Java and save the
 *   Prescription, Hibernate deletes that row from the database too
 *   (instead of leaving an orphaned row with a dangling reference).
 */
@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    // Nullable: a doctor might issue a prescription during a visit that
    // is captured in a MedicalRecord, but we don't want to force that
    // link if a prescription is ever created independently.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id")
    private MedicalRecord medicalRecord;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate = LocalDate.now();

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionItem> items = new ArrayList<>();

    public Prescription() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public MedicalRecord getMedicalRecord() {
        return medicalRecord;
    }

    public void setMedicalRecord(MedicalRecord medicalRecord) {
        this.medicalRecord = medicalRecord;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public List<PrescriptionItem> getItems() {
        return items;
    }

    public void setItems(List<PrescriptionItem> items) {
        this.items = items;
    }

    // A small helper method (not required by JPA) that keeps both
    // sides of the relationship in sync when adding an item. Without
    // this, it's easy to forget to call item.setPrescription(this)
    // and end up with a PrescriptionItem that doesn't know which
    // Prescription it belongs to.
    public void addItem(PrescriptionItem item) {
        items.add(item);
        item.setPrescription(this);
    }
}
