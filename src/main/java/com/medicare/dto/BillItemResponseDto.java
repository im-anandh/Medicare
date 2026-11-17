package com.medicare.dto;

import java.math.BigDecimal;

public class BillItemResponseDto {

    private Long id;
    private String description;
    private BigDecimal amount;

    public BillItemResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
