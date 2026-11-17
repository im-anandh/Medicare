package com.medicare.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Body of PUT /api/prescriptions/{id}.
 *
 * Only the medicine list can change. Which medical record, patient and
 * doctor a prescription belongs to is fixed once it is issued, so those
 * fields are deliberately not accepted here (a separate DTO from the
 * create one makes that impossible to get wrong).
 *
 * PUT semantics: the list you send REPLACES the old list entirely.
 */
public class PrescriptionUpdateRequestDto {

    @NotEmpty(message = "At least one medicine is required")
    @Valid
    private List<PrescriptionItemRequestDto> items;

    public PrescriptionUpdateRequestDto() {
    }

    public List<PrescriptionItemRequestDto> getItems() {
        return items;
    }

    public void setItems(List<PrescriptionItemRequestDto> items) {
        this.items = items;
    }
}
