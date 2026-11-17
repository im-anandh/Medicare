package com.medicare.service.impl;

import com.medicare.dto.*;
import com.medicare.entity.*;
import com.medicare.exception.AppointmentConflictException;
import com.medicare.exception.InvalidRequestException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.AppointmentRepository;
import com.medicare.repository.DoctorRepository;
import com.medicare.repository.PatientRepository;
import com.medicare.security.CurrentUserService;
import com.medicare.service.AppointmentService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Transactional here for the same reason as DoctorServiceImpl: our
 * response DTO nests DoctorResponseDto, which itself needs to read a
 * LAZY Department - so the whole conversion needs to happen inside
 * one open transaction.
 */
@Service
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final CurrentUserService currentUserService;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                   PatientRepository patientRepository,
                                   DoctorRepository doctorRepository,
                                   CurrentUserService currentUserService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.currentUserService = currentUserService;
    }

    @Override
    public List<AppointmentResponseDto> getAllAppointments(Long doctorId, Long patientId, LocalDate date) {
        Role role = currentUserService.getCurrentRole();

        // A DOCTOR or PATIENT can only ever see THEIR OWN appointments
        // - we deliberately IGNORE any doctorId/patientId the client
        // passed in and force it to their own linked id instead. This
        // matters for security: without this override, a logged-in
        // patient could pass ?patientId=999 and read someone else's
        // schedule just by changing a query parameter.
        if (role == Role.DOCTOR) {
            Long ownDoctorId = currentUserService.getCurrentDoctorId();
            if (ownDoctorId == null) {
                return List.of();
            }
            doctorId = ownDoctorId;
            patientId = null;
        } else if (role == Role.PATIENT) {
            Long ownPatientId = currentUserService.getCurrentPatientId();
            if (ownPatientId == null) {
                return List.of();
            }
            patientId = ownPatientId;
            doctorId = null;
        }
        // ADMIN/RECEPTIONIST: use whatever filters the client supplied
        // (or none, for the full list), unchanged.

        List<Appointment> appointments;
        if (doctorId != null && date != null) {
            appointments = appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, date);
        } else if (doctorId != null) {
            appointments = appointmentRepository.findByDoctorId(doctorId);
        } else if (patientId != null) {
            appointments = appointmentRepository.findByPatientId(patientId);
        } else {
            appointments = appointmentRepository.findAll();
        }

        return appointments.stream().map(this::toResponseDto).toList();
    }

    @Override
    public AppointmentResponseDto getAppointmentById(Long id) {
        Appointment appointment = findAppointmentOrThrow(id);
        assertCanAccessAppointment(appointment);
        return toResponseDto(appointment);
    }

    @Override
    public AppointmentResponseDto bookAppointment(AppointmentRequestDto dto) {
        validateNotInPast(dto.getAppointmentDate(), dto.getAppointmentTime());

        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + dto.getPatientId()));
        Doctor doctor = doctorRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + dto.getDoctorId()));

        boolean conflict = appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatus(
                dto.getDoctorId(), dto.getAppointmentDate(), dto.getAppointmentTime(), AppointmentStatus.SCHEDULED);
        if (conflict) {
            throw new AppointmentConflictException(
                    "Dr. " + doctor.getName() + " already has an appointment at "
                            + dto.getAppointmentDate() + " " + dto.getAppointmentTime());
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(dto.getAppointmentDate());
        appointment.setAppointmentTime(dto.getAppointmentTime());
        appointment.setReason(dto.getReason());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        return toResponseDto(appointmentRepository.save(appointment));
    }

    @Override
    public AppointmentResponseDto rescheduleAppointment(Long id, RescheduleRequestDto dto) {
        Appointment appointment = findAppointmentOrThrow(id);
        assertCanAccessAppointment(appointment);

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new InvalidRequestException(
                    "Only SCHEDULED appointments can be rescheduled. Current status: " + appointment.getStatus());
        }

        validateNotInPast(dto.getAppointmentDate(), dto.getAppointmentTime());

        boolean conflict = appointmentRepository
                .existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusAndIdNot(
                        appointment.getDoctor().getId(), dto.getAppointmentDate(), dto.getAppointmentTime(),
                        AppointmentStatus.SCHEDULED, appointment.getId());
        if (conflict) {
            throw new AppointmentConflictException(
                    "Dr. " + appointment.getDoctor().getName() + " already has an appointment at "
                            + dto.getAppointmentDate() + " " + dto.getAppointmentTime());
        }

        appointment.setAppointmentDate(dto.getAppointmentDate());
        appointment.setAppointmentTime(dto.getAppointmentTime());

        // The patient may already have been reminded about the OLD time.
        // Clear the flags so the reminder scheduler texts them again for
        // the NEW time (otherwise they would never hear about the move).
        appointment.setReminderSent(false);
        appointment.setReminderSentAt(null);

        return toResponseDto(appointmentRepository.save(appointment));
    }

    @Override
    public AppointmentResponseDto cancelAppointment(Long id) {
        Appointment appointment = findAppointmentOrThrow(id);
        assertCanAccessAppointment(appointment);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new InvalidRequestException("Appointment is already cancelled");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        return toResponseDto(appointmentRepository.save(appointment));
    }

    // ---------- Private helpers ----------

    private Appointment findAppointmentOrThrow(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
    }

    // ADMIN/RECEPTIONIST manage all appointments (per spec). DOCTOR
    // can only touch their own appointments; PATIENT can only touch
    // their own appointments (view - booking/rescheduling by patients
    // isn't exposed by SecurityConfig at all right now, so this check
    // mainly matters for a future PATIENT-facing booking feature).
    private void assertCanAccessAppointment(Appointment appointment) {
        Role role = currentUserService.getCurrentRole();
        if (role == Role.ADMIN || role == Role.RECEPTIONIST) {
            return;
        }
        if (role == Role.DOCTOR) {
            Long ownDoctorId = currentUserService.getCurrentDoctorId();
            if (!appointment.getDoctor().getId().equals(ownDoctorId)) {
                throw new AccessDeniedException("You can only access your own appointments");
            }
            return;
        }
        if (role == Role.PATIENT) {
            Long ownPatientId = currentUserService.getCurrentPatientId();
            if (!appointment.getPatient().getId().equals(ownPatientId)) {
                throw new AccessDeniedException("You can only access your own appointments");
            }
        }
    }

    // Combines date + time into one LocalDateTime and compares it
    // against the actual current moment - the cross-field check we
    // discussed in AppointmentRequestDto's comments.
    private void validateNotInPast(LocalDate date, java.time.LocalTime time) {
        LocalDateTime requested = LocalDateTime.of(date, time);
        if (requested.isBefore(LocalDateTime.now())) {
            throw new InvalidRequestException("Appointment date/time cannot be in the past");
        }
    }

    private AppointmentResponseDto toResponseDto(Appointment appointment) {
        AppointmentResponseDto dto = new AppointmentResponseDto();
        dto.setId(appointment.getId());
        dto.setAppointmentDate(appointment.getAppointmentDate());
        dto.setAppointmentTime(appointment.getAppointmentTime());
        dto.setReason(appointment.getReason());
        dto.setStatus(appointment.getStatus());
        dto.setCreatedAt(appointment.getCreatedAt());
        dto.setReminderSent(appointment.isReminderSent());
        dto.setPatient(toPatientDto(appointment.getPatient()));
        dto.setDoctor(toDoctorDto(appointment.getDoctor()));
        return dto;
    }

    private PatientResponseDto toPatientDto(Patient patient) {
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

    private DoctorResponseDto toDoctorDto(Doctor doctor) {
        DoctorResponseDto dto = new DoctorResponseDto();
        dto.setId(doctor.getId());
        dto.setName(doctor.getName());
        dto.setSpecialization(doctor.getSpecialization());
        dto.setPhone(doctor.getPhone());
        dto.setEmail(doctor.getEmail());
        dto.setExperience(doctor.getExperience());
        dto.setAvailability(doctor.getAvailability());
        dto.setActive(doctor.isActive());

        Department department = doctor.getDepartment();
        DepartmentResponseDto departmentDto = new DepartmentResponseDto();
        departmentDto.setId(department.getId());
        departmentDto.setName(department.getName());
        departmentDto.setDescription(department.getDescription());
        departmentDto.setActive(department.isActive());
        dto.setDepartment(departmentDto);

        return dto;
    }
}
