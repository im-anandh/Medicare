package com.medicare.repository;

import com.medicare.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // Matches doctors whose name OR specialization contains the search
    // text (case-insensitive) - lets a receptionist search "cardio" and
    // find both a doctor named "Dr. Cardiff" and any Cardiology doctor.
    List<Doctor> findByNameContainingIgnoreCaseOrSpecializationContainingIgnoreCase(
            String name, String specialization);

    Optional<Doctor> findByUser_Username(String username);

    // Used by the bulk import to report a duplicate email against the exact
    // item. IgnoreCase because MySQL's unique index treats "A@x.com" and
    // "a@x.com" as the same value.
    boolean existsByEmailIgnoreCase(String email);
}
