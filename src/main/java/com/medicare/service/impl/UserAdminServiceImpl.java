package com.medicare.service.impl;

import com.medicare.dto.CreateUserRequestDto;
import com.medicare.dto.ResetPasswordRequestDto;
import com.medicare.dto.UserResponseDto;
import com.medicare.entity.Doctor;
import com.medicare.entity.Patient;
import com.medicare.entity.Role;
import com.medicare.entity.User;
import com.medicare.exception.InvalidRequestException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.DoctorRepository;
import com.medicare.repository.PatientRepository;
import com.medicare.repository.UserRepository;
import com.medicare.security.CurrentUserService;
import com.medicare.service.ProfileLinker;
import com.medicare.service.UserAdminService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WHO MAY DO WHAT
 *   ADMIN        - list all accounts, create an account of ANY role,
 *                  reset anyone's password
 *   RECEPTIONIST - create PATIENT accounts only (front-desk onboarding)
 *   everyone else - nothing
 *
 * This is the only way to create ADMIN, DOCTOR and RECEPTIONIST logins,
 * now that public registration is limited to patients. (The very first
 * ADMIN of a fresh database comes from AdminBootstrapRunner.)
 * SecurityConfig already restricts the URLs; the checks here are a
 * second line of defence in case that configuration is ever loosened.
 */
@Service
@Transactional
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ProfileLinker profileLinker;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    public UserAdminServiceImpl(UserRepository userRepository,
                                PatientRepository patientRepository,
                                DoctorRepository doctorRepository,
                                ProfileLinker profileLinker,
                                PasswordEncoder passwordEncoder,
                                CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.profileLinker = profileLinker;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
    }

    @Override
    public List<UserResponseDto> getAllUsers() {
        assertAdmin();

        // Which Patient / Doctor record does each login belong to? The
        // link is stored on the profile side (patients.user_id,
        // doctors.user_id), so build a lookup by user id first.
        Map<Long, Patient> patientByUserId = new HashMap<>();
        for (Patient p : patientRepository.findAll()) {
            if (p.getUser() != null) {
                patientByUserId.put(p.getUser().getId(), p);
            }
        }
        Map<Long, Doctor> doctorByUserId = new HashMap<>();
        for (Doctor d : doctorRepository.findAll()) {
            if (d.getUser() != null) {
                doctorByUserId.put(d.getUser().getId(), d);
            }
        }

        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(u -> toDto(u, patientByUserId.get(u.getId()), doctorByUserId.get(u.getId())))
                .toList();
    }

    @Override
    public UserResponseDto createUser(CreateUserRequestDto dto) {
        Role caller = currentUserService.getCurrentRole();
        if (caller == Role.RECEPTIONIST) {
            if (dto.getRole() != Role.PATIENT) {
                throw new AccessDeniedException("Receptionists can only create patient logins");
            }
        } else if (caller != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN or RECEPTIONIST can create logins");
        }

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new InvalidRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new InvalidRequestException("Email is already registered");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        User saved = userRepository.save(user);

        profileLinker.link(dto.getRole(), dto.getProfileId(), saved);

        Patient patient = null;
        Doctor doctor = null;
        if (dto.getRole() == Role.PATIENT) {
            patient = patientRepository.findById(dto.getProfileId()).orElse(null);
        } else if (dto.getRole() == Role.DOCTOR) {
            doctor = doctorRepository.findById(dto.getProfileId()).orElse(null);
        }
        return toDto(saved, patient, doctor);
    }

    @Override
    public void resetPassword(Long userId, ResetPasswordRequestDto dto) {
        assertAdmin();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    private void assertAdmin() {
        if (currentUserService.getCurrentRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can manage accounts");
        }
    }

    private UserResponseDto toDto(User user, Patient patient, Doctor doctor) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setActive(user.isActive());
        dto.setCreatedAt(user.getCreatedAt());
        if (patient != null) {
            dto.setProfileId(patient.getId());
            dto.setProfileName(patient.getFullName());
        } else if (doctor != null) {
            dto.setProfileId(doctor.getId());
            dto.setProfileName(doctor.getName());
        }
        return dto;
    }
}
