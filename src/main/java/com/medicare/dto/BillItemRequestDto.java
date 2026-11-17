package com.medicare.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * One extra line on a bill, e.g. "X-ray chest" - 450.00.
 *
 * Money is ALWAYS BigDecimal, never double/float: floating point cannot
 * represent values like 0.10 exactly, so sums drift (0.1 + 0.2 =
 * 0.30000000000000004). BigDecimal does exact decimal arithmetic.
 *
 * @Digits(integer = 8, fraction = 2) matches the database column
 * decimal(10,2): at most 8 digits before the point and 2 after.
 */
public class BillItemRequestDto {

    @NotBlank(message = "Item description is required")
    @Size(max = 255, message = "Item description must be at most 255 characters")
    private String description;

    @NotNull(message = "Item amount is required")
    @DecimalMin(value = "0.01", message = "Item amount must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Item amount can have at most 8 digits and 2 decimals")
    private BigDecimal amount;

    public BillItemRequestDto() {
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
