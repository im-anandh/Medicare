package com.medicare.controller;

import com.medicare.dto.PrescriptionRequestDto;
import com.medicare.dto.PrescriptionResponseDto;
import com.medicare.dto.PrescriptionUpdateRequestDto;
import com.medicare.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    // Scoped by the service: a doctor gets the prescriptions they
    // issued, a patient gets their own.
    @GetMapping
    public ResponseEntity<List<PrescriptionResponseDto>> getAllPrescriptions() {
        return ResponseEntity.ok(prescriptionService.getAllPrescriptions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionResponseDto> getPrescriptionById(@PathVariable Long id) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionById(id));
    }

    @PostMapping
    public ResponseEntity<PrescriptionResponseDto> createPrescription(
            @Valid @RequestBody PrescriptionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prescriptionService.createPrescription(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrescriptionResponseDto> updatePrescription(
            @PathVariable Long id, @Valid @RequestBody PrescriptionUpdateRequestDto dto) {
        return ResponseEntity.ok(prescriptionService.updatePrescription(id, dto));
    }
}
