package com.medicare.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * The part of a bill that staff can set or change: the five charge
 * categories plus any extra line items. This is the body of
 * PUT /api/bills/{id}, and BillRequestDto (create) extends it.
 *
 * There is deliberately NO totalAmount field. The server always
 * computes the total itself (all charges + all item amounts), so a
 * client can never submit a total that disagrees with the lines.
 *
 * Every charge is optional (a missing one counts as 0), but the final
 * total must be greater than zero - checked in the service.
 */
public class BillUpdateRequestDto {

    @DecimalMin(value = "0.00", message = "Consultation charge cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Consultation charge can have at most 8 digits and 2 decimals")
    private BigDecimal consultationCharge;

    @DecimalMin(value = "0.00", message = "Lab charge cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Lab charge can have at most 8 digits and 2 decimals")
    private BigDecimal labCharge;

    @DecimalMin(value = "0.00", message = "Medicine charge cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Medicine charge can have at most 8 digits and 2 decimals")
    private BigDecimal medicineCharge;

    @DecimalMin(value = "0.00", message = "Procedure charge cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Procedure charge can have at most 8 digits and 2 decimals")
    private BigDecimal procedureCharge;

    @DecimalMin(value = "0.00", message = "Other charge cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Other charge can have at most 8 digits and 2 decimals")
    private BigDecimal otherCharge;

    // @Valid steps into each item so its own annotations are checked.
    @Valid
    @Size(max = 50, message = "A bill can have at most 50 extra items")
    private List<BillItemRequestDto> items;

    public BillUpdateRequestDto() {
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

    public List<BillItemRequestDto> getItems() {
        return items;
    }

    public void setItems(List<BillItemRequestDto> items) {
        this.items = items;
    }
}
