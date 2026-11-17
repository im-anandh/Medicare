package com.medicare.service;

import com.medicare.dto.DepartmentRequestDto;
import com.medicare.dto.DepartmentResponseDto;
import java.util.List;

public interface DepartmentService {

    List<DepartmentResponseDto> getAllDepartments();

    DepartmentResponseDto getDepartmentById(Long id);

    DepartmentResponseDto createDepartment(DepartmentRequestDto dto);

    DepartmentResponseDto updateDepartment(Long id, DepartmentRequestDto dto);

    void deactivateDepartment(Long id);
}
