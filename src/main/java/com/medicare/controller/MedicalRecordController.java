package com.medicare.controller;

import com.medicare.dto.MedicalRecordRequestDto;
import com.medicare.dto.MedicalRecordResponseDto;
import com.medicare.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    @GetMapping
    public ResponseEntity<List<MedicalRecordResponseDto>> getAllMedicalRecords(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId) {
        return ResponseEntity.ok(medicalRecordService.getAllMedicalRecords(patientId, doctorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecordResponseDto> getMedicalRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecordById(id));
    }

    // Restricted to DOCTOR role in SecurityConfig - matches the spec's
    // permission table ("Doctors: ... Add diagnosis, Add medical
    // notes"). Admins/receptionists can still VIEW records (the GET
    // endpoints above), just not create clinical content themselves.
    @PostMapping
    public ResponseEntity<MedicalRecordResponseDto> createMedicalRecord(
            @Valid @RequestBody MedicalRecordRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicalRecordService.createMedicalRecord(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalRecordResponseDto> updateMedicalRecord(
            @PathVariable Long id, @Valid @RequestBody MedicalRecordRequestDto dto) {
        return ResponseEntity.ok(medicalRecordService.updateMedicalRecord(id, dto));
    }
}
