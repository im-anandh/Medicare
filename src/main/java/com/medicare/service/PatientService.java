package com.medicare.service;

import com.medicare.dto.PatientRequestDto;
import com.medicare.dto.PatientResponseDto;
import java.util.List;

/**
 * Why an interface AND an implementation class, instead of just one
 * class? Two reasons that matter in practice:
 *
 * 1. The Controller depends on this interface, not on the concrete
 *    PatientServiceImpl class. This means we could swap in a
 *    different implementation later (e.g. a cached version, or a
 *    mock for testing) without changing a single line of the
 *    Controller.
 * 2. It documents, in one place, exactly what operations exist on
 *    "patients" - readable as a contract, without implementation
 *    details cluttering it.
 */
public interface PatientService {

    List<PatientResponseDto> getAllPatients();

    PatientResponseDto getPatientById(Long id);

    PatientResponseDto createPatient(PatientRequestDto dto);

    PatientResponseDto updatePatient(Long id, PatientRequestDto dto);

    void deactivatePatient(Long id);

    List<PatientResponseDto> searchPatients(String name);

    // Convenience for a logged-in PATIENT to fetch their own record
    // without needing to already know their own database id.
    PatientResponseDto getMyProfile();
}
