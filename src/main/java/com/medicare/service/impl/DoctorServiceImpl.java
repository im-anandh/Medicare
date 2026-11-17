package com.medicare.service.impl;

import com.medicare.dto.DepartmentResponseDto;
import com.medicare.dto.DoctorRequestDto;
import com.medicare.dto.DoctorResponseDto;
import com.medicare.entity.Department;
import com.medicare.entity.Doctor;
import com.medicare.entity.Role;
import com.medicare.exception.InvalidRequestException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.repository.DepartmentRepository;
import com.medicare.repository.DoctorRepository;
import com.medicare.security.CurrentUserService;
import com.medicare.service.DoctorService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Notice the constructor takes TWO repositories. A service is free to
 * depend on as many repositories as the operation genuinely needs -
 * here, creating a Doctor requires looking up an existing Department,
 * so this service naturally needs access to both tables.
 *
 * WHY @Transactional IS NEEDED HERE (a new concept):
 * Doctor.department is fetch = FetchType.LAZY (we set this in Phase
 * 2). That means when Hibernate loads a Doctor row, it does NOT
 * immediately load its Department too - it waits until something
 * actually calls doctor.getDepartment(). That call only works while
 * a database connection/session is still open for that entity.
 *
 * Our application.properties has spring.jpa.open-in-view=false
 * (a deliberate best-practice setting from Phase 1 - leaving it
 * "true", the default, silently keeps a database connection open for
 * the ENTIRE web request, which wastes connections and hides bugs
 * like this one). With it false, a repository call's session closes
 * the moment that repository method returns.
 *
 * toResponseDto() below calls doctor.getDepartment() - and if that
 * happens AFTER the session has already closed, Hibernate throws
 * LazyInitializationException. @Transactional on the service method
 * keeps one session open for the WHOLE method, so by the time we
 * convert to DTO, the department is still safely loadable.
 */
@Service
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final CurrentUserService currentUserService;

    public DoctorServiceImpl(DoctorRepository doctorRepository, DepartmentRepository departmentRepository,
                              CurrentUserService currentUserService) {
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.currentUserService = currentUserService;
    }

    @Override
    public List<DoctorResponseDto> getAllDoctors() {
        return doctorRepository.findAll().stream().map(this::toResponseDto).toList();
    }

    @Override
    public DoctorResponseDto getDoctorById(Long id) {
        return toResponseDto(findDoctorOrThrow(id));
    }

    @Override
    public DoctorResponseDto createDoctor(DoctorRequestDto dto) {
        Doctor doctor = new Doctor();
        applyDtoToEntity(dto, doctor);
        return toResponseDto(doctorRepository.save(doctor));
    }

    @Override
    public DoctorResponseDto updateDoctor(Long id, DoctorRequestDto dto) {
        Doctor doctor = findDoctorOrThrow(id);
        applyDtoToEntity(dto, doctor);
        return toResponseDto(doctorRepository.save(doctor));
    }

    @Override
    public void deactivateDoctor(Long id) {
        Doctor doctor = findDoctorOrThrow(id);
        doctor.setActive(false);
        doctorRepository.save(doctor);
    }

    @Override
    public List<DoctorResponseDto> searchDoctors(String query) {
        return doctorRepository
                .findByNameContainingIgnoreCaseOrSpecializationContainingIgnoreCase(query, query)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public DoctorResponseDto updateAvailability(Long id, String availability) {
        Doctor doctor = findDoctorOrThrow(id);

        // SecurityConfig lets ADMIN and DOCTOR reach this endpoint. A doctor
        // may only change THEIR OWN availability - never a colleague's.
        Role role = currentUserService.getCurrentRole();
        if (role == Role.DOCTOR) {
            Long ownId = currentUserService.getCurrentDoctorId();
            if (ownId == null || !ownId.equals(id)) {
                throw new AccessDeniedException("You can only update your own availability");
            }
        } else if (role != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN or the doctor themself can change availability");
        }
        if (availability != null && availability.length() > 255) {
            throw new InvalidRequestException("Availability must be at most 255 characters");
        }

        doctor.setAvailability(availability);
        return toResponseDto(doctorRepository.save(doctor));
    }

    @Override
    public DoctorResponseDto getMyProfile() {
        Long ownId = currentUserService.getCurrentDoctorId();
        if (ownId == null) {
            throw new AccessDeniedException("Your login is not linked to a doctor profile");
        }
        return getDoctorById(ownId);
    }

    // ---------- Private helpers ----------

    private Doctor findDoctorOrThrow(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));
    }

    private void applyDtoToEntity(DoctorRequestDto dto, Doctor doctor) {
        doctor.setName(dto.getName());
        doctor.setSpecialization(dto.getSpecialization());
        doctor.setPhone(dto.getPhone());
        doctor.setEmail(dto.getEmail());
        doctor.setExperience(dto.getExperience());
        doctor.setAvailability(dto.getAvailability());

        // This is the key step: we don't just store dto.getDepartmentId()
        // on the doctor - a Doctor's "department" field expects an
        // actual Department OBJECT (that's what @ManyToOne means). So
        // we look up the real row from the database first. If no
        // department exists with that id, this throws
        // ResourceNotFoundException immediately, which our
        // GlobalExceptionHandler turns into a clean 404 - meaning a
        // client can never accidentally create a doctor pointing at a
        // department that doesn't exist.
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id: " + dto.getDepartmentId()));
        doctor.setDepartment(department);
    }

    private DoctorResponseDto toResponseDto(Doctor doctor) {
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
