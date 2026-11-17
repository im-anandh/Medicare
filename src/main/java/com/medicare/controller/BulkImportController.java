package com.medicare.controller;

import com.medicare.dto.DoctorRequestDto;
import com.medicare.dto.DoctorResponseDto;
import com.medicare.dto.PatientRequestDto;
import com.medicare.dto.PatientResponseDto;
import com.medicare.service.BulkImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bulk-create endpoints. They live in their own controller so the
 * existing PatientController and DoctorController stay untouched.
 *
 * Access is decided by SecurityConfig purely from the URL:
 *   POST /api/patients/**  -> ADMIN, RECEPTIONIST   (covers /patients/bulk)
 *   POST /api/doctors/**   -> ADMIN only            (covers /doctors/bulk)
 * so no new security rule is needed.
 *
 * The body is a plain JSON ARRAY of the same objects the single-create
 * endpoints accept. There is no @Valid here on purpose: the items inside
 * an array are validated by BulkRequestValidator in the service.
 */
@RestController
@RequestMapping("/api")
public class BulkImportController {

    private final BulkImportService bulkImportService;

    public BulkImportController(BulkImportService bulkImportService) {
        this.bulkImportService = bulkImportService;
    }

    @PostMapping("/patients/bulk")
    public ResponseEntity<List<PatientResponseDto>> createPatients(
            @RequestBody List<PatientRequestDto> patients) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bulkImportService.importPatients(patients));
    }

    @PostMapping("/doctors/bulk")
    public ResponseEntity<List<DoctorResponseDto>> createDoctors(
            @RequestBody List<DoctorRequestDto> doctors) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bulkImportService.importDoctors(doctors));
    }
}
