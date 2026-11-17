package com.medicare.security;

import com.medicare.entity.Role;
import com.medicare.entity.User;
import com.medicare.repository.DoctorRepository;
import com.medicare.repository.PatientRepository;
import com.medicare.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * A small helper that answers "who is making this request, and what
 * are they allowed to see?" from anywhere in our service layer.
 *
 * SecurityContextHolder.getContext().getAuthentication().getName()
 * gives us the CURRENTLY LOGGED IN username for whatever request is
 * being processed right now - this works because our
 * JwtAuthenticationFilter (built in Phase 6) already populated it
 * from the request's JWT before this code ever runs. We don't need
 * to pass "who's calling" as a parameter through every method - it's
 * available globally for the duration of one request, tied to the
 * current thread.
 */
@Component
public class CurrentUserService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public CurrentUserService(UserRepository userRepository,
                               PatientRepository patientRepository,
                               DoctorRepository doctorRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    public String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public Role getCurrentRole() {
        User user = userRepository.findByUsername(getCurrentUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
        return user.getRole();
    }

    /**
     * Returns the Patient id linked to the current login, or null if
     * the current user isn't a PATIENT (or has no linked profile yet).
     * Callers should treat null as "this user has no patient
     * ownership to check against."
     */
    public Long getCurrentPatientId() {
        return patientRepository.findByUser_Username(getCurrentUsername())
                .map(patient -> patient.getId())
                .orElse(null);
    }

    /**
     * Same idea, for the DOCTOR role.
     */
    public Long getCurrentDoctorId() {
        return doctorRepository.findByUser_Username(getCurrentUsername())
                .map(doctor -> doctor.getId())
                .orElse(null);
    }
}
