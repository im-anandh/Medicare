package com.medicare.controller;

import com.medicare.dto.AppointmentRequestDto;
import com.medicare.dto.AppointmentResponseDto;
import com.medicare.dto.RescheduleRequestDto;
import com.medicare.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // All three query params are optional - this single endpoint
    // covers "view all appointments", "view a doctor's appointments",
    // "view a patient's appointments", and "check a doctor's
    // availability on a given date" (by supplying doctorId + date and
    // seeing what's already booked).
    // @DateTimeFormat tells Spring how to parse the date query param
    // (e.g. "2026-08-20") into a real LocalDate object.
    @GetMapping
    public ResponseEntity<List<AppointmentResponseDto>> getAllAppointments(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.getAllAppointments(doctorId, patientId, date));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDto> getAppointmentById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @PostMapping
    public ResponseEntity<AppointmentResponseDto> bookAppointment(
            @Valid @RequestBody AppointmentRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.bookAppointment(dto));
    }

    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentResponseDto> rescheduleAppointment(
            @PathVariable Long id, @Valid @RequestBody RescheduleRequestDto dto) {
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(id, dto));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponseDto> cancelAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }
}
