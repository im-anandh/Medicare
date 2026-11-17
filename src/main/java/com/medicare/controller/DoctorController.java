package com.medicare.controller;

import com.medicare.dto.DoctorRequestDto;
import com.medicare.dto.DoctorResponseDto;
import com.medicare.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public ResponseEntity<List<DoctorResponseDto>> getAllDoctors(
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(doctorService.searchDoctors(search));
        }
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }

    @GetMapping("/me")
    public ResponseEntity<DoctorResponseDto> getMyProfile() {
        return ResponseEntity.ok(doctorService.getMyProfile());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponseDto> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

    @PostMapping
    public ResponseEntity<DoctorResponseDto> createDoctor(@Valid @RequestBody DoctorRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.createDoctor(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DoctorResponseDto> updateDoctor(
            @PathVariable Long id, @Valid @RequestBody DoctorRequestDto dto) {
        return ResponseEntity.ok(doctorService.updateDoctor(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateDoctor(@PathVariable Long id) {
        doctorService.deactivateDoctor(id);
        return ResponseEntity.noContent().build();
    }

    // PATCH is the correct HTTP method for a PARTIAL update - we're
    // only changing one field (availability), not replacing the whole
    // doctor record the way PUT implies. The request body is a simple
    // {"availability": "..."} object, read here as a Map for
    // simplicity rather than creating a whole new DTO class for one
    // field.
    @PatchMapping("/{id}/availability")
    public ResponseEntity<DoctorResponseDto> updateAvailability(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        String availability = body.get("availability");
        return ResponseEntity.ok(doctorService.updateAvailability(id, availability));
    }
}
