package com.medicare.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Note on money: we use BigDecimal, never double or float, for currency
 * amounts. Floating point types like double cannot represent decimal
 * fractions like 0.10 exactly in binary, which causes tiny rounding
 * errors that compound over many calculations - unacceptable for
 * billing. BigDecimal represents decimal numbers exactly.
 */
@Entity
@Table(name = "bills")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // Optional: a bill may be tied to a specific appointment/visit.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(name = "consultation_charge", precision = 10, scale = 2)
    private BigDecimal consultationCharge = BigDecimal.ZERO;

    @Column(name = "lab_charge", precision = 10, scale = 2)
    private BigDecimal labCharge = BigDecimal.ZERO;

    @Column(name = "medicine_charge", precision = 10, scale = 2)
    private BigDecimal medicineCharge = BigDecimal.ZERO;

    @Column(name = "procedure_charge", precision = 10, scale = 2)
    private BigDecimal procedureCharge = BigDecimal.ZERO;

    @Column(name = "other_charge", precision = 10, scale = 2)
    private BigDecimal otherCharge = BigDecimal.ZERO;

    // IMPORTANT (per the project spec): this total will always be
    // CALCULATED by the service layer from the charges above, never
    // trusted if it were to arrive from the frontend. We store it here
    // so we don't have to recompute it on every read.
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate = LocalDate.now();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BillItem> items = new ArrayList<>();

    public Bill() {
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

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public BigDecimal getConsultationCharge() {
        return consultationCharge;
    }

    public void setConsultationCharge(BigDecimal consultationCharge) {
        this.consultationCharge = consultationCharge;
    }

    public BigDecimal getLabCharge() {
        return labCharge;
    }

    public void setLabCharge(BigDecimal labCharge) {
        this.labCharge = labCharge;
    }

    public BigDecimal getMedicineCharge() {
        return medicineCharge;
    }

    public void setMedicineCharge(BigDecimal medicineCharge) {
        this.medicineCharge = medicineCharge;
    }

    public BigDecimal getProcedureCharge() {
        return procedureCharge;
    }

    public void setProcedureCharge(BigDecimal procedureCharge) {
        this.procedureCharge = procedureCharge;
    }

    public BigDecimal getOtherCharge() {
        return otherCharge;
    }

    public void setOtherCharge(BigDecimal otherCharge) {
        this.otherCharge = otherCharge;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public List<BillItem> getItems() {
        return items;
    }

    public void setItems(List<BillItem> items) {
        this.items = items;
    }

    public void addItem(BillItem item) {
        items.add(item);
        item.setBill(this);
    }
}
