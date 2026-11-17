package com.medicare.service.impl;

import com.medicare.dto.*;
import com.medicare.entity.*;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.DoctorRepository;
import com.medicare.repository.MedicalRecordRepository;
import com.medicare.repository.PatientRepository;
import com.medicare.security.CurrentUserService;
import com.medicare.service.MedicalRecordService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Transactional again, for the same reason as AppointmentServiceImpl
 * and DoctorServiceImpl: our response DTO nests DoctorResponseDto,
 * which needs its LAZY Department loaded while we're still inside an
 * open session.
 *
 * ACCESS MODEL FOR THIS RESOURCE (per your requirements):
 * Only DOCTOR (their own records) and PATIENT (their own records) can
 * read medical records at all - ADMIN and RECEPTIONIST are blocked
 * entirely at the SecurityConfig gate, since clinical notes are more
 * sensitive than basic contact/appointment info. Only DOCTOR can
 * write.
 */
@Service
@Transactional
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final CurrentUserService currentUserService;

    public MedicalRecordServiceImpl(MedicalRecordRepository medicalRecordRepository,
                                     PatientRepository patientRepository,
                                     DoctorRepository doctorRepository,
                                     CurrentUserService currentUserService) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.currentUserService = currentUserService;
    }

    @Override
    public List<MedicalRecordResponseDto> getAllMedicalRecords(Long patientId, Long doctorId) {
        Role role = currentUserService.getCurrentRole();

        // Same principle as AppointmentServiceImpl.getAllAppointments:
        // force the filter to the caller's own linked id, ignoring
        // whatever the client passed in, so a doctor or patient can
        // never widen the query to see someone else's records.
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
        // ADMIN/RECEPTIONIST never reach this method at all -
        // SecurityConfig blocks them before the request gets here.

        List<MedicalRecord> records;
        if (patientId != null) {
            records = medicalRecordRepository.findByPatientId(patientId);
        } else if (doctorId != null) {
            records = medicalRecordRepository.findByDoctorId(doctorId);
        } else {
            records = medicalRecordRepository.findAll();
        }
        return records.stream().map(this::toResponseDto).toList();
    }

    @Override
    public MedicalRecordResponseDto getMedicalRecordById(Long id) {
        MedicalRecord record = findRecordOrThrow(id);
        assertCanAccessRecord(record);
        return toResponseDto(record);
    }

    @Override
    public MedicalRecordResponseDto createMedicalRecord(MedicalRecordRequestDto dto) {
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + dto.getPatientId()));

        // SECURITY IMPROVEMENT: rather than trusting dto.getDoctorId()
        // from the request body, we resolve it from the CURRENTLY
        // LOGGED IN doctor. Without this, any doctor could submit
        // someone else's doctorId in the JSON body and create a
        // record that appears to be written by a colleague. A doctor
        // can only ever create records under their own identity.
        Long doctorId = currentUserService.getCurrentDoctorId();
        if (doctorId == null) {
            throw new AccessDeniedException("Your login is not linked to a doctor profile");
        }
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + doctorId));

        MedicalRecord record = new MedicalRecord();
        record.setPatient(patient);
        record.setDoctor(doctor);
        applyDtoFields(dto, record);

        return toResponseDto(medicalRecordRepository.save(record));
    }

    @Override
    public MedicalRecordResponseDto updateMedicalRecord(Long id, MedicalRecordRequestDto dto) {
        MedicalRecord record = findRecordOrThrow(id);
        assertCanAccessRecord(record);
        // Note: we deliberately don't allow changing WHICH patient or
        // doctor a record belongs to during an update - only its
        // clinical content. Re-pointing a medical record to a
        // different patient after the fact would be a serious data
        // integrity problem, not a normal edit.
        applyDtoFields(dto, record);
        return toResponseDto(medicalRecordRepository.save(record));
    }

    // ---------- Private helpers ----------

    private MedicalRecord findRecordOrThrow(Long id) {
        return medicalRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found with id: " + id));
    }

    private void assertCanAccessRecord(MedicalRecord record) {
        Role role = currentUserService.getCurrentRole();
        if (role == Role.DOCTOR) {
            Long ownDoctorId = currentUserService.getCurrentDoctorId();
            if (!record.getDoctor().getId().equals(ownDoctorId)) {
                throw new AccessDeniedException("You can only access medical records you created");
            }
        } else if (role == Role.PATIENT) {
            Long ownPatientId = currentUserService.getCurrentPatientId();
            if (!record.getPatient().getId().equals(ownPatientId)) {
                throw new AccessDeniedException("You can only access your own medical records");
            }
        }
        // ADMIN/RECEPTIONIST never reach here - blocked earlier.
    }

    private void applyDtoFields(MedicalRecordRequestDto dto, MedicalRecord record) {
        record.setVisitDate(dto.getVisitDate());
        record.setSymptoms(dto.getSymptoms());
        record.setDiagnosis(dto.getDiagnosis());
        record.setTreatment(dto.getTreatment());
        record.setMedicalNotes(dto.getMedicalNotes());
        record.setFollowUpDate(dto.getFollowUpDate());
    }

    private MedicalRecordResponseDto toResponseDto(MedicalRecord record) {
        MedicalRecordResponseDto dto = new MedicalRecordResponseDto();
        dto.setId(record.getId());
        dto.setVisitDate(record.getVisitDate());
        dto.setSymptoms(record.getSymptoms());
        dto.setDiagnosis(record.getDiagnosis());
        dto.setTreatment(record.getTreatment());
        dto.setMedicalNotes(record.getMedicalNotes());
        dto.setFollowUpDate(record.getFollowUpDate());
        dto.setPatient(toPatientDto(record.getPatient()));
        dto.setDoctor(toDoctorDto(record.getDoctor()));
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
