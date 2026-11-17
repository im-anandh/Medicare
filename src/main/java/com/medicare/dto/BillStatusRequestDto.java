package com.medicare.dto;

import com.medicare.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;

/** Body of PATCH /api/bills/{id}/status, e.g. {"status": "PAID"}. */
public class BillStatusRequestDto {

    @NotNull(message = "status is required")
    private PaymentStatus status;

    public BillStatusRequestDto() {
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
}
