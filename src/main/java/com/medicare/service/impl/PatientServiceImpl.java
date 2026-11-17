package com.medicare.service.impl;

import com.medicare.dto.PatientRequestDto;
import com.medicare.dto.PatientResponseDto;
import com.medicare.entity.Patient;
import com.medicare.entity.Role;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.AppointmentRepository;
import com.medicare.repository.PatientRepository;
import com.medicare.security.CurrentUserService;
import com.medicare.service.PatientService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Service marks this as a Spring-managed bean belonging to the
 * "service" layer - functionally identical to @Component, but the
 * distinct name documents intent and lets certain Spring tooling
 * treat it specially.
 *
 * DEPENDENCY INJECTION IN ACTION:
 * Notice the constructor below takes a PatientRepository parameter,
 * and we just... store it. We never call "new PatientRepository()"
 * anywhere - remember, PatientRepository is an interface with no
 * concrete class! Spring creates the actual implementation at
 * startup (as explained in PatientRepository.java) and automatically
 * "injects" it here because this class asks for it in its
 * constructor. This pattern - declaring what you need as a
 * constructor parameter, and letting Spring supply it - is called
 * CONSTRUCTOR INJECTION, and it's the recommended way to wire
 * dependencies in modern Spring (preferred over older
 * @Autowired-on-a-field style, because it makes dependencies
 * explicit and the class easy to unit test).
 *
 * OWNERSHIP-BASED ACCESS CONTROL (new in this update):
 * Every read method below now checks getCurrentRole() and narrows
 * what's returned accordingly. This is DIFFERENT from the role
 * checks in SecurityConfig - SecurityConfig only answers "is this
 * role allowed to call this URL at all?" It has no idea that a
 * PATIENT calling GET /api/patients should only see themselves, or
 * that a DOCTOR should only see patients they've actually treated.
 * That's business logic specific to what "patient" means in OUR
 * domain, so it belongs here, in the service layer.
 */
@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final CurrentUserService currentUserService;

    public PatientServiceImpl(PatientRepository patientRepository,
                               AppointmentRepository appointmentRepository,
                               CurrentUserService currentUserService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.currentUserService = currentUserService;
    }

    @Override
    public List<PatientResponseDto> getAllPatients() {
        Role role = currentUserService.getCurrentRole();

        if (role == Role.PATIENT) {
            // A patient's "list of patients" is just themselves.
            Long ownId = currentUserService.getCurrentPatientId();
            if (ownId == null) {
                return List.of();
            }
            return patientRepository.findById(ownId)
                    .map(p -> List.of(toResponseDto(p)))
                    .orElse(List.of());
        }

        if (role == Role.DOCTOR) {
            Long doctorId = currentUserService.getCurrentDoctorId();
            if (doctorId == null) {
                return List.of();
            }
            List<Long> patientIds = appointmentRepository.findDistinctPatientIdsByDoctorId(doctorId);
            return patientRepository.findAllById(patientIds)
                    .stream().map(this::toResponseDto).toList();
        }

        // ADMIN and RECEPTIONIST see everyone.
        return patientRepository.findAll().stream().map(this::toResponseDto).toList();
    }

    @Override
    public PatientResponseDto getPatientById(Long id) {
        Patient patient = findPatientOrThrow(id);
        assertCanAccessPatient(id);
        return toResponseDto(patient);
    }

    @Override
    public PatientResponseDto createPatient(PatientRequestDto dto) {
        Patient patient = new Patient();
        applyDtoToEntity(dto, patient);
        Patient saved = patientRepository.save(patient);
        return toResponseDto(saved);
    }

    @Override
    public PatientResponseDto updatePatient(Long id, PatientRequestDto dto) {
        Patient patient = findPatientOrThrow(id);
        applyDtoToEntity(dto, patient);
        Patient saved = patientRepository.save(patient);
        return toResponseDto(saved);
    }

    @Override
    public void deactivatePatient(Long id) {
        Patient patient = findPatientOrThrow(id);
        // We deliberately never hard-delete a patient row: medical,
        // billing, and prescription history must be preserved for
        // legal/record-keeping reasons even after a patient is no
        // longer active. This is why the spec calls this operation
        // "Deactivate patient" rather than "Delete patient" - we
        // simply flip the active flag instead of removing the row.
        patient.setActive(false);
        patientRepository.save(patient);
    }

    @Override
    public List<PatientResponseDto> searchPatients(String name) {
        // Search is only meaningful for ADMIN/RECEPTIONIST (staff
        // looking someone up). PATIENT/DOCTOR roles fall back to their
        // already-scoped list instead of searching the whole table.
        Role role = currentUserService.getCurrentRole();
        if (role == Role.PATIENT || role == Role.DOCTOR) {
            return getAllPatients();
        }
        return patientRepository.findByFullNameContainingIgnoreCase(name)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public PatientResponseDto getMyProfile() {
        Long ownId = currentUserService.getCurrentPatientId();
        if (ownId == null) {
            throw new AccessDeniedException("Your login is not linked to a patient profile");
        }
        return getPatientById(ownId);
    }

    // ---------- Private helpers ----------

    private Patient findPatientOrThrow(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
    }

    // Throws AccessDeniedException (caught by GlobalExceptionHandler,
    // turned into a 403) if the current user isn't allowed to view
    // this specific patient.
    private void assertCanAccessPatient(Long patientId) {
        Role role = currentUserService.getCurrentRole();

        if (role == Role.ADMIN || role == Role.RECEPTIONIST) {
            return; // staff can view anyone
        }

        if (role == Role.PATIENT) {
            Long ownId = currentUserService.getCurrentPatientId();
            if (!patientId.equals(ownId)) {
                throw new AccessDeniedException("You can only access your own patient record");
            }
            return;
        }

        if (role == Role.DOCTOR) {
            Long doctorId = currentUserService.getCurrentDoctorId();
            List<Long> treatedPatientIds = appointmentRepository.findDistinctPatientIdsByDoctorId(doctorId);
            if (!treatedPatientIds.contains(patientId)) {
                throw new AccessDeniedException("You can only access patients you have treated");
            }
        }
    }

    // Copies fields from an incoming request DTO onto an entity.
    // Used for both create (new empty entity) and update (existing
    // entity loaded from the database).
    private void applyDtoToEntity(PatientRequestDto dto, Patient patient) {
        patient.setFullName(dto.getFullName());
        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setGender(dto.getGender());
        patient.setBloodGroup(dto.getBloodGroup());
        patient.setPhone(dto.getPhone());
        patient.setEmail(dto.getEmail());
        patient.setAddress(dto.getAddress());
        patient.setEmergencyContact(dto.getEmergencyContact());
    }

    // Converts an entity into the DTO shape we're willing to expose
    // over the API - this is the one place that decides "these are
    // the fields a client is allowed to see."
    private PatientResponseDto toResponseDto(Patient patient) {
        PatientResponseDto dto = new PatientResponseDto();
        dto.setId(patient.getId());
        dto.setFullName(patient.getFullName());
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setGender(patient.getGender());
        dto.setBloodGroup(patient.getBloodGroup());
        dto.setPhone(patient.getPhone());
        dto.setEmail(patient.getEmail());
        dto.setAddress(patient.getAddress());
        dto.setEmergencyContact(patient.getEmergencyContact());
        dto.setCreatedAt(patient.getCreatedAt());
        dto.setActive(patient.isActive());
        return dto;
    }
}
