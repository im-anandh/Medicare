package com.medicare.service;

import com.medicare.entity.Doctor;
import com.medicare.entity.Patient;
import com.medicare.entity.Role;
import com.medicare.entity.User;
import com.medicare.exception.InvalidRequestException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.DoctorRepository;
import com.medicare.repository.PatientRepository;
import org.springframework.stereotype.Component;

/**
 * Connects a new login (User) to the Patient or Doctor profile row it
 * belongs to - the "link by ID" rule that the whole ownership model is
 * built on. It used to be a private method inside AuthServiceImpl; it
 * lives here now so that BOTH ways of creating a login (public patient
 * sign-up, and an administrator creating an account) share exactly one
 * implementation of the rule.
 *
 * It must be called from inside a @Transactional method: it changes
 * managed entities, and if it throws, the caller's transaction rolls
 * back the half-created User too.
 */
@Component
public class ProfileLinker {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public ProfileLinker(PatientRepository patientRepository, DoctorRepository doctorRepository) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    public void link(Role role, Long profileId, User user) {
        if (role == Role.PATIENT) {
            if (profileId == null) {
                throw new InvalidRequestException(
                        "profileId is required when creating a PATIENT login - "
                                + "the patient record must exist first");
            }
            Patient patient = patientRepository.findById(profileId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No patient profile found with id: " + profileId));
            if (patient.getUser() != null) {
                throw new InvalidRequestException("This patient profile already has a login linked to it");
            }
            patient.setUser(user);
            patientRepository.save(patient);

        } else if (role == Role.DOCTOR) {
            if (profileId == null) {
                throw new InvalidRequestException(
                        "profileId is required when creating a DOCTOR login - "
                                + "the doctor record must exist first");
            }
            Doctor doctor = doctorRepository.findById(profileId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No doctor profile found with id: " + profileId));
            if (doctor.getUser() != null) {
                throw new InvalidRequestException("This doctor profile already has a login linked to it");
            }
            doctor.setUser(user);
            doctorRepository.save(doctor);
        }
        // ADMIN and RECEPTIONIST have no separate profile table - their
        // User row IS their whole identity.
    }
}
