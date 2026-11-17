package com.medicare.service;

import com.medicare.dto.DoctorRequestDto;
import com.medicare.dto.DoctorResponseDto;
import com.medicare.dto.PatientRequestDto;
import com.medicare.dto.PatientResponseDto;

import java.util.List;

/**
 * Create many patients or doctors in ONE request.
 * All-or-nothing: if any item is invalid, nothing is saved and the
 * error lists every problem. Results come back in request order.
 */
public interface BulkImportService {

    List<PatientResponseDto> importPatients(List<PatientRequestDto> patients);

    List<DoctorResponseDto> importDoctors(List<DoctorRequestDto> doctors);
}
