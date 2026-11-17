package com.medicare.service;

import com.medicare.dto.AppointmentRequestDto;
import com.medicare.dto.AppointmentResponseDto;
import com.medicare.dto.RescheduleRequestDto;
import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    List<AppointmentResponseDto> getAllAppointments(Long doctorId, Long patientId, LocalDate date);

    AppointmentResponseDto getAppointmentById(Long id);

    AppointmentResponseDto bookAppointment(AppointmentRequestDto dto);

    AppointmentResponseDto rescheduleAppointment(Long id, RescheduleRequestDto dto);

    AppointmentResponseDto cancelAppointment(Long id);
}
