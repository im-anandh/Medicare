package com.medicare.service.impl;

import com.medicare.dto.PrescriptionItemRequestDto;
import com.medicare.dto.PrescriptionItemResponseDto;
import com.medicare.dto.PrescriptionRequestDto;
import com.medicare.dto.PrescriptionResponseDto;
import com.medicare.dto.PrescriptionUpdateRequestDto;
import com.medicare.entity.MedicalRecord;
import com.medicare.entity.Prescription;
import com.medicare.entity.PrescriptionItem;
import com.medicare.entity.Role;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.mapper.EntityMapper;
import com.medicare.repository.MedicalRecordRepository;
import com.medicare.repository.PrescriptionRepository;
import com.medicare.security.CurrentUserService;
import com.medicare.service.PrescriptionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ACCESS MODEL (same spirit as medical records):
 *   DOCTOR   - create/read/edit only prescriptions they issued
 *   PATIENT  - read-only, only their own
 *   ADMIN / RECEPTIONIST - no access (blocked in SecurityConfig, and
 *              blocked AGAIN here in getAllPrescriptions as a second
 *              safety net in case that config is ever loosened)
 *
 * A prescription is always issued against one of the doctor's own
 * medical records (a "visit"). That single rule gives us the
 * doctor-patient relationship for free: the patient comes from the
 * record, so no separate "has this doctor treated this patient?" check
 * is needed.
 */
@Service
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final CurrentUserService currentUserService;
    private final EntityMapper mapper;

    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository,
                                    MedicalRecordRepository medicalRecordRepository,
                                    CurrentUserService currentUserService,
                                    EntityMapper mapper) {
        this.prescriptionRepository = prescriptionRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.currentUserService = currentUserService;
        this.mapper = mapper;
    }

    @Override
    public List<PrescriptionResponseDto> getAllPrescriptions() {
        Role role = currentUserService.getCurrentRole();
        List<Prescription> prescriptions;

        if (role == Role.DOCTOR) {
            Long ownDoctorId = currentUserService.getCurrentDoctorId();
            if (ownDoctorId == null) {
                return List.of();
            }
            prescriptions = prescriptionRepository.findByDoctorIdOrderByCreatedDateDescIdDesc(ownDoctorId);
        } else if (role == Role.PATIENT) {
            Long ownPatientId = currentUserService.getCurrentPatientId();
            if (ownPatientId == null) {
                return List.of();
            }
            prescriptions = prescriptionRepository.findByPatientIdOrderByCreatedDateDescIdDesc(ownPatientId);
        } else {
            throw new AccessDeniedException("Your role cannot access prescriptions");
        }

        return prescriptions.stream().map(this::toResponseDto).toList();
    }

    @Override
    public PrescriptionResponseDto getPrescriptionById(Long id) {
        Prescription prescription = findPrescriptionOrThrow(id);
        assertCanView(prescription);
        return toResponseDto(prescription);
    }

    @Override
    public PrescriptionResponseDto createPrescription(PrescriptionRequestDto dto) {
        Long doctorId = currentUserService.getCurrentDoctorId();
        if (doctorId == null) {
            throw new AccessDeniedException("Your login is not linked to a doctor profile");
        }

        MedicalRecord record = medicalRecordRepository.findById(dto.getMedicalRecordId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Medical record not found with id: " + dto.getMedicalRecordId()));

        if (!record.getDoctor().getId().equals(doctorId)) {
            throw new AccessDeniedException("You can only prescribe against medical records you created");
        }

        Prescription prescription = new Prescription();
        prescription.setMedicalRecord(record);
        prescription.setPatient(record.getPatient());
        prescription.setDoctor(record.getDoctor());
        addItems(prescription, dto.getItems());

        // CascadeType.ALL on Prescription.items means saving the
        // prescription also saves every item - no separate item
        // repository or save call is needed.
        return toResponseDto(prescriptionRepository.save(prescription));
    }

    @Override
    public PrescriptionResponseDto updatePrescription(Long id, PrescriptionUpdateRequestDto dto) {
        Prescription prescription = findPrescriptionOrThrow(id);

        // Editing is stricter than viewing: only the doctor who issued
        // it. Checked by doctor id (not by role) so it holds even if a
        // role rule elsewhere changes.
        Long ownDoctorId = currentUserService.getCurrentDoctorId();
        if (ownDoctorId == null || !prescription.getDoctor().getId().equals(ownDoctorId)) {
            throw new AccessDeniedException("You can only edit prescriptions you issued");
        }

        // Replace the medicine list. IMPORTANT: we clear() the EXISTING
        // list and add to it, rather than doing setItems(newList).
        // Prescription.items is mapped with orphanRemoval = true, which
        // means Hibernate tracks that exact list object and deletes any
        // item removed from it. Swapping in a brand-new list object
        // makes Hibernate throw "A collection with cascade=all-delete-
        // orphan was no longer referenced". clear() + add keeps the
        // same object, so the old rows are deleted and the new ones
        // inserted cleanly.
        prescription.getItems().clear();
        addItems(prescription, dto.getItems());

        return toResponseDto(prescriptionRepository.save(prescription));
    }

    // ---------------- helpers ----------------

    private Prescription findPrescriptionOrThrow(Long id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with id: " + id));
    }

    private void assertCanView(Prescription prescription) {
        Role role = currentUserService.getCurrentRole();
        if (role == Role.DOCTOR) {
            Long ownDoctorId = currentUserService.getCurrentDoctorId();
            if (!prescription.getDoctor().getId().equals(ownDoctorId)) {
                throw new AccessDeniedException("You can only access prescriptions you issued");
            }
        } else if (role == Role.PATIENT) {
            Long ownPatientId = currentUserService.getCurrentPatientId();
            if (!prescription.getPatient().getId().equals(ownPatientId)) {
                throw new AccessDeniedException("You can only access your own prescriptions");
            }
        } else {
            throw new AccessDeniedException("Your role cannot access prescriptions");
        }
    }

    // addItem() is a helper on the Prescription entity that adds the
    // item to the list AND sets item.prescription to point back, keeping
    // both sides of the relationship in sync. The database foreign key
    // (prescription_id) is written from the item's side, so forgetting
    // that back-pointer would try to insert items with no prescription.
    private void addItems(Prescription prescription, List<PrescriptionItemRequestDto> itemDtos) {
        for (PrescriptionItemRequestDto itemDto : itemDtos) {
            PrescriptionItem item = new PrescriptionItem();
            item.setMedicine(itemDto.getMedicine());
            item.setDosage(itemDto.getDosage());
            item.setFrequency(itemDto.getFrequency());
            item.setDuration(itemDto.getDuration());
            item.setInstructions(itemDto.getInstructions());
            prescription.addItem(item);
        }
    }

    private PrescriptionResponseDto toResponseDto(Prescription prescription) {
        PrescriptionResponseDto dto = new PrescriptionResponseDto();
        dto.setId(prescription.getId());
        dto.setCreatedDate(prescription.getCreatedDate());
        if (prescription.getMedicalRecord() != null) {
            dto.setMedicalRecordId(prescription.getMedicalRecord().getId());
        }
        dto.setPatient(mapper.toPatientDto(prescription.getPatient()));
        dto.setDoctor(mapper.toDoctorDto(prescription.getDoctor()));
        dto.setItems(prescription.getItems().stream().map(this::toItemDto).toList());
        return dto;
    }

    private PrescriptionItemResponseDto toItemDto(PrescriptionItem item) {
        PrescriptionItemResponseDto dto = new PrescriptionItemResponseDto();
        dto.setId(item.getId());
        dto.setMedicine(item.getMedicine());
        dto.setDosage(item.getDosage());
        dto.setFrequency(item.getFrequency());
        dto.setDuration(item.getDuration());
        dto.setInstructions(item.getInstructions());
        return dto;
    }
}
