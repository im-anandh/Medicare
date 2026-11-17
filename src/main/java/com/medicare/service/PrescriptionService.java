package com.medicare.service;

import com.medicare.dto.PrescriptionRequestDto;
import com.medicare.dto.PrescriptionResponseDto;
import com.medicare.dto.PrescriptionUpdateRequestDto;

import java.util.List;

public interface PrescriptionService {

    List<PrescriptionResponseDto> getAllPrescriptions();

    PrescriptionResponseDto getPrescriptionById(Long id);

    PrescriptionResponseDto createPrescription(PrescriptionRequestDto dto);

    PrescriptionResponseDto updatePrescription(Long id, PrescriptionUpdateRequestDto dto);
}
