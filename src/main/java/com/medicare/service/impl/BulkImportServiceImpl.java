package com.medicare.service.impl;

import com.medicare.dto.DoctorRequestDto;
import com.medicare.dto.DoctorResponseDto;
import com.medicare.dto.PatientRequestDto;
import com.medicare.dto.PatientResponseDto;
import com.medicare.entity.Department;
import com.medicare.exception.BulkValidationException;
import com.medicare.repository.DepartmentRepository;
import com.medicare.repository.DoctorRepository;
import com.medicare.service.BulkImportService;
import com.medicare.service.DoctorService;
import com.medicare.service.PatientService;
import com.medicare.util.BulkRequestValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deliberately built on top of the existing PatientService and
 * DoctorService: each item is saved by the very same createPatient /
 * createDoctor code the single-create endpoints use, so there is one
 * definition of "how a patient/doctor is created".
 *
 * @Transactional on the class = the whole batch is ONE database
 * transaction. If anything fails part-way (even something unexpected
 * like a database error on item 7), every item already saved in this
 * request is rolled back. The checks below make that unlikely, and the
 * transaction guarantees it can never leave a half-imported batch.
 */
@Service
@Transactional
public class BulkImportServiceImpl implements BulkImportService {

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final BulkRequestValidator bulkValidator;

    public BulkImportServiceImpl(PatientService patientService,
                                 DoctorService doctorService,
                                 DoctorRepository doctorRepository,
                                 DepartmentRepository departmentRepository,
                                 BulkRequestValidator bulkValidator) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.bulkValidator = bulkValidator;
    }

    @Override
    public List<PatientResponseDto> importPatients(List<PatientRequestDto> patients) {
        bulkValidator.requireValidSize(patients);
        failIfAny(bulkValidator.validateItems(patients));

        List<PatientResponseDto> created = new ArrayList<>();
        for (PatientRequestDto dto : patients) {
            created.add(patientService.createPatient(dto));
        }
        return created;
    }

    @Override
    public List<DoctorResponseDto> importDoctors(List<DoctorRequestDto> doctors) {
        bulkValidator.requireValidSize(doctors);
        Map<String, String> errors = bulkValidator.validateItems(doctors);

        // Checks the DTO annotations cannot do, so that a bad department
        // or a duplicate email is reported against the exact item (instead
        // of a vague 404/409 for the whole request).
        //
        // 1) every referenced department must exist - looked up in ONE query
        Set<Long> wantedDepartmentIds = doctors.stream()
                .filter(Objects::nonNull)
                .map(DoctorRequestDto::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> existingDepartmentIds = departmentRepository.findAllById(wantedDepartmentIds).stream()
                .map(Department::getId)
                .collect(Collectors.toSet());

        // 2) emails must be unique: against the database AND within this
        //    request (the database column is unique, and MySQL compares
        //    case-insensitively, so "A@x.com" and "a@x.com" clash).
        Map<String, Integer> firstSeenAt = new HashMap<>();

        for (int i = 0; i < doctors.size(); i++) {
            DoctorRequestDto dto = doctors.get(i);
            if (dto == null) {
                continue;
            }
            if (dto.getDepartmentId() != null && !existingDepartmentIds.contains(dto.getDepartmentId())) {
                errors.putIfAbsent("[" + i + "].departmentId",
                        "Department not found with id: " + dto.getDepartmentId());
            }
            String email = dto.getEmail();
            if (email != null && !email.isBlank()) {
                Integer firstIndex = firstSeenAt.putIfAbsent(email.toLowerCase(), i);
                if (firstIndex != null) {
                    errors.putIfAbsent("[" + i + "].email", "Duplicate of item [" + firstIndex + "] in this request");
                } else if (doctorRepository.existsByEmailIgnoreCase(email)) {
                    errors.putIfAbsent("[" + i + "].email", "A doctor with this email already exists");
                }
            }
        }
        failIfAny(errors);

        List<DoctorResponseDto> created = new ArrayList<>();
        for (DoctorRequestDto dto : doctors) {
            created.add(doctorService.createDoctor(dto));
        }
        return created;
    }

    private void failIfAny(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new BulkValidationException(errors);
        }
    }
}
