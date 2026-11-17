package com.medicare.repository;

import com.medicare.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring builds the queries from the method NAMES - no SQL needed:
 *
 *   findByPatientId          ->  WHERE patient.id = ?
 *   OrderByCreatedDateDesc   ->  ORDER BY created_date DESC
 *   IdDesc                   ->  ... then by id DESC (so two prescriptions
 *                                 issued the same day still have a stable,
 *                                 newest-first order)
 */
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByPatientIdOrderByCreatedDateDescIdDesc(Long patientId);

    List<Prescription> findByDoctorIdOrderByCreatedDateDescIdDesc(Long doctorId);
}
