package com.medicare.service;

import com.medicare.dto.MedicalRecordRequestDto;
import com.medicare.dto.MedicalRecordResponseDto;
import java.util.List;

public interface MedicalRecordService {

    List<MedicalRecordResponseDto> getAllMedicalRecords(Long patientId, Long doctorId);

    MedicalRecordResponseDto getMedicalRecordById(Long id);

    MedicalRecordResponseDto createMedicalRecord(MedicalRecordRequestDto dto);

    MedicalRecordResponseDto updateMedicalRecord(Long id, MedicalRecordRequestDto dto);
}
