package com.medicare.dto;

import com.medicare.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BillResponseDto {

    private Long id;
    private LocalDate createdDate;
    private Long appointmentId;
    private PatientResponseDto patient;
    private BigDecimal consultationCharge;
    private BigDecimal labCharge;
    private BigDecimal medicineCharge;
    private BigDecimal procedureCharge;
    private BigDecimal otherCharge;
    private List<BillItemResponseDto> items;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;

    public BillResponseDto() {
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

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public PatientResponseDto getPatient() {
        return patient;
    }

    public void setPatient(PatientResponseDto patient) {
        this.patient = patient;
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

    public List<BillItemResponseDto> getItems() {
        return items;
    }

    public void setItems(List<BillItemResponseDto> items) {
        this.items = items;
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
}
