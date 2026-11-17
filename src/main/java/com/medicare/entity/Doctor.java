package com.medicare.entity;

import jakarta.persistence.*;

/**
 * RELATIONSHIP ANNOTATIONS EXPLAINED (used throughout this project):
 *
 * @ManyToOne
 *   "Many doctors can belong to one department." This goes on the SIDE
 *   THAT HOLDS THE FOREIGN KEY - here, the doctors table gets a
 *   department_id column pointing back to a row in departments.
 *
 * @JoinColumn(name = "department_id")
 *   Names the actual foreign key column in the database. Without this,
 *   Hibernate would pick a default name itself - being explicit makes
 *   the schema predictable and easy to read in MySQL Workbench.
 *
 * fetch = FetchType.LAZY
 *   Tells Hibernate: "don't automatically load the related Department
 *   every time you load a Doctor - only fetch it if the code actually
 *   asks for doctor.getDepartment()." This avoids pulling in more data
 *   than needed and is the recommended default for @ManyToOne in real
 *   applications. (LAZY is already the JPA default for @OneToMany, but
 *   @ManyToOne defaults to EAGER, so we set it explicitly here.)
 *
 * We are keeping relationships ONE-DIRECTIONAL for now (Doctor points
 * to Department, but Department does not hold a list of its Doctors).
 * This keeps things simple while we're just learning the mappings.
 * If we ever need "give me all doctors in this department" as a
 * query, the repository layer can do that with a simple method,
 * without needing a bidirectional relationship.
 */
@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String specialization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, unique = true)
    private String email;

    // Years of experience
    private int experience;

    // A simple text description for now, e.g. "Mon-Fri, 9am-5pm".
    // We will model this more richly (as actual time slots) only if a
    // later phase needs to calculate free/busy slots automatically.
    private String availability;

    @Column(nullable = false)
    private boolean active = true;

    // Optional link to a login account. Nullable because a Doctor row
    // could theoretically exist before their login is created (e.g.
    // an admin adds the doctor's profile first, invites them to set
    // up their account later). We will enforce and use this properly
    // once Phase 6 (auth) is built.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    public Doctor() {
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

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
