package com.medicare.controller;

import com.medicare.dto.PatientRequestDto;
import com.medicare.dto.PatientResponseDto;
import com.medicare.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Notice how thin this class is - it has NO business logic. Its only
 * job is: receive the HTTP request, hand it to the service, and wrap
 * the result in the right HTTP status code. This is exactly the
 * separation of concerns we discussed - if you ever need to change
 * a business rule, you'll be editing PatientServiceImpl, never this
 * class.
 *
 * @RequestMapping("/api/patients") sets a shared URL prefix for
 * every method below, so we only write the REMAINING part of each
 * path (e.g. "/{id}" instead of "/api/patients/{id}").
 *
 * @Valid on a method parameter tells Spring "run this DTO's
 * validation annotations before entering the method body." If
 * validation fails, Spring throws MethodArgumentNotValidException
 * automatically - which our GlobalExceptionHandler catches and turns
 * into a clean 400 response, so this controller doesn't need an if
 * statement checking validity anywhere.
 */
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public ResponseEntity<List<PatientResponseDto>> getAllPatients(
            @RequestParam(required = false) String search) {
        // Supports the "Search patient" requirement via an optional
        // query param, e.g. GET /api/patients?search=anand
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(patientService.searchPatients(search));
        }
        return ResponseEntity.ok(patientService.getAllPatients());
    }

    @GetMapping("/me")
    public ResponseEntity<PatientResponseDto> getMyProfile() {
        return ResponseEntity.ok(patientService.getMyProfile());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponseDto> getPatientById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    @PostMapping
    public ResponseEntity<PatientResponseDto> createPatient(@Valid @RequestBody PatientRequestDto dto) {
        PatientResponseDto created = patientService.createPatient(dto);
        // 201 CREATED is the correct status for a successful POST that
        // creates a new resource - 200 OK would be misleading here.
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientResponseDto> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequestDto dto) {
        return ResponseEntity.ok(patientService.updatePatient(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivatePatient(@PathVariable Long id) {
        patientService.deactivatePatient(id);
        // 204 NO CONTENT: the request succeeded, but there's nothing
        // meaningful to send back in the response body.
        return ResponseEntity.noContent().build();
    }
}
