package com.medicare.dto;

/**
 * Here, the RESPONSE nests a full DepartmentResponseDto (not just an
 * id) - the opposite direction from the request DTO. This is
 * intentional and common: when WRITING, a client only needs to name
 * which department by id; when READING, it's far more useful to get
 * the department's actual name directly, instead of forcing the
 * client to make a second API call just to display it.
 */
public class DoctorResponseDto {

    private Long id;
    private String name;
    private String specialization;
    private DepartmentResponseDto department;
    private String phone;
    private String email;
    private int experience;
    private String availability;
    private boolean active;

    public DoctorResponseDto() {
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

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public DepartmentResponseDto getDepartment() {
        return department;
    }

    public void setDepartment(DepartmentResponseDto department) {
        this.department = department;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
