package com.medicare.service.impl;

import com.medicare.dto.DepartmentRequestDto;
import com.medicare.dto.DepartmentResponseDto;
import com.medicare.entity.Department;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.DepartmentRepository;
import com.medicare.service.DepartmentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public List<DepartmentResponseDto> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public DepartmentResponseDto getDepartmentById(Long id) {
        return toResponseDto(findDepartmentOrThrow(id));
    }

    @Override
    public DepartmentResponseDto createDepartment(DepartmentRequestDto dto) {
        Department department = new Department();
        department.setName(dto.getName());
        department.setDescription(dto.getDescription());
        return toResponseDto(departmentRepository.save(department));
    }

    @Override
    public DepartmentResponseDto updateDepartment(Long id, DepartmentRequestDto dto) {
        Department department = findDepartmentOrThrow(id);
        department.setName(dto.getName());
        department.setDescription(dto.getDescription());
        return toResponseDto(departmentRepository.save(department));
    }

    @Override
    public void deactivateDepartment(Long id) {
        Department department = findDepartmentOrThrow(id);
        department.setActive(false);
        departmentRepository.save(department);
    }

    private Department findDepartmentOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    private DepartmentResponseDto toResponseDto(Department department) {
        DepartmentResponseDto dto = new DepartmentResponseDto();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());
        dto.setActive(department.isActive());
        return dto;
    }
}
