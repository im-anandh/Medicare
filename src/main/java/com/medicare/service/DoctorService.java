package com.medicare.service;

import com.medicare.dto.DoctorRequestDto;
import com.medicare.dto.DoctorResponseDto;
import java.util.List;

public interface DoctorService {

    List<DoctorResponseDto> getAllDoctors();

    DoctorResponseDto getDoctorById(Long id);

    DoctorResponseDto createDoctor(DoctorRequestDto dto);

    DoctorResponseDto updateDoctor(Long id, DoctorRequestDto dto);

    void deactivateDoctor(Long id);

    List<DoctorResponseDto> searchDoctors(String query);

    DoctorResponseDto updateAvailability(Long id, String availability);

    // Convenience for a logged-in DOCTOR to fetch their own profile.
    DoctorResponseDto getMyProfile();
}
