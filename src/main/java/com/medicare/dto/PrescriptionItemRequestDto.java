package com.medicare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One medicine line inside a prescription, e.g.
 * "Paracetamol 500mg - 1 tablet - 3 times a day - 5 days".
 *
 * The @Size(max = 255) limits match the database columns (varchar(255)).
 * Without them, an over-long value would get past validation and then
 * fail at the database with a confusing error instead of a clear 400
 * telling the user which field is too long.
 */
public class PrescriptionItemRequestDto {

    @NotBlank(message = "Medicine name is required")
    @Size(max = 255, message = "Medicine name must be at most 255 characters")
    private String medicine;

    @NotBlank(message = "Dosage is required")
    @Size(max = 255, message = "Dosage must be at most 255 characters")
    private String dosage;

    @NotBlank(message = "Frequency is required")
    @Size(max = 255, message = "Frequency must be at most 255 characters")
    private String frequency;

    @NotBlank(message = "Duration is required")
    @Size(max = 255, message = "Duration must be at most 255 characters")
    private String duration;

    // Optional, e.g. "Take after food"
    @Size(max = 255, message = "Instructions must be at most 255 characters")
    private String instructions;

    public PrescriptionItemRequestDto() {
    }

    public String getMedicine() {
        return medicine;
    }

    public void setMedicine(String medicine) {
        this.medicine = medicine;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
