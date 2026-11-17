package com.medicare.mapper;

import com.medicare.dto.DepartmentResponseDto;
import com.medicare.dto.DoctorResponseDto;
import com.medicare.dto.PatientResponseDto;
import com.medicare.entity.Department;
import com.medicare.entity.Doctor;
import com.medicare.entity.Patient;
import org.springframework.stereotype.Component;

/**
 * The shared mapper we flagged as a cleanup candidate: one home for the
 * "entity -> response DTO" conversions that AppointmentServiceImpl,
 * MedicalRecordServiceImpl and DoctorServiceImpl each carry their own
 * copy of.
 *
 * To keep Phase 8 low-risk, ONLY the new prescription code uses this.
 * The three older services are untouched and still work; switching
 * them over to this class later is a safe, mechanical cleanup.
 *
 * Important: toDoctorDto reads doctor.getDepartment(), which is a LAZY
 * relationship. That is fine because this class is only ever called
 * from inside a @Transactional service method, where the database
 * session is still open (open-in-view is off in this project).
 */
@Component
public class EntityMapper {

    public PatientResponseDto toPatientDto(Patient patient) {
        PatientResponseDto dto = new PatientResponseDto();
        dto.setId(patient.getId());
        dto.setFullName(patient.getFullName());
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setGender(patient.getGender());
        dto.setBloodGroup(patient.getBloodGroup());
        dto.setPhone(patient.getPhone());
        dto.setEmail(patient.getEmail());
        dto.setAddress(patient.getAddress());
        dto.setEmergencyContact(patient.getEmergencyContact());
        dto.setCreatedAt(patient.getCreatedAt());
        dto.setActive(patient.isActive());
        return dto;
    }

    public DoctorResponseDto toDoctorDto(Doctor doctor) {
        DoctorResponseDto dto = new DoctorResponseDto();
        dto.setId(doctor.getId());
        dto.setName(doctor.getName());
        dto.setSpecialization(doctor.getSpecialization());
        dto.setPhone(doctor.getPhone());
        dto.setEmail(doctor.getEmail());
        dto.setExperience(doctor.getExperience());
        dto.setAvailability(doctor.getAvailability());
        dto.setActive(doctor.isActive());

        Department department = doctor.getDepartment();
        DepartmentResponseDto departmentDto = new DepartmentResponseDto();
        departmentDto.setId(department.getId());
        departmentDto.setName(department.getName());
        departmentDto.setDescription(department.getDescription());
        departmentDto.setActive(department.isActive());
        dto.setDepartment(departmentDto);

        return dto;
    }
}
