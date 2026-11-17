package com.medicare.repository;

import com.medicare.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * This is the most "magic" part of Spring Data JPA, so let's be
 * precise about what's actually happening.
 *
 * This is an INTERFACE with no implementation written anywhere. Yet
 * at runtime, Spring generates a full working implementation class
 * behind the scenes and injects it wherever PatientRepository is
 * needed. This works because:
 *
 * 1. JpaRepository<Patient, Long> already provides a full set of
 *    ready-made methods just by extending it: save(), findById(),
 *    findAll(), deleteById(), count(), and more. Patient = the entity
 *    type, Long = the type of its @Id field.
 *
 * 2. For anything beyond the basics, Spring Data JPA can derive a
 *    full SQL query just from a method's NAME, following a naming
 *    convention. findByFullNameContainingIgnoreCase(String name)
 *    below gets translated automatically into:
 *
 *      SELECT * FROM patients
 *      WHERE LOWER(full_name) LIKE LOWER('%' || ? || '%')
 *
 *    "findBy" + "FullName" (a field on Patient) + "Containing"
 *    (LIKE %...%) + "IgnoreCase" (case-insensitive). No SQL, no
 *    annotation, no implementation - just a correctly-named method
 *    signature.
 */
public interface PatientRepository extends JpaRepository<Patient, Long> {

    List<Patient> findByFullNameContainingIgnoreCase(String name);

    // Spring Data JPA reads "User_Username" as "traverse the 'user'
    // relationship on Patient, then match its 'username' field" -
    // the underscore lets us cross a @OneToOne relationship in a
    // derived query, without writing any JPQL ourselves.
    Optional<Patient> findByUser_Username(String username);

}
