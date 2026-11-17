package com.medicare.dto;

/**
 * Notice this is a small, flat DTO with no relationships of its own -
 * that's deliberate. We're about to reuse THIS exact class as a
 * nested field inside DoctorResponseDto (a doctor "contains" a
 * summary of its department). Keeping DepartmentResponseDto simple
 * means that nesting stays simple too - it never risks pulling in a
 * department's own list of doctors, which would create a circular
 * "doctor -> department -> doctors -> department -> ..." loop when
 * Jackson tries to convert it to JSON.
 */
public class DepartmentResponseDto {

    private Long id;
    private String name;
    private String description;
    private boolean active;

    public DepartmentResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
