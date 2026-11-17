package com.medicare.service.impl;

import com.medicare.dto.BillItemRequestDto;
import com.medicare.dto.BillItemResponseDto;
import com.medicare.dto.BillRequestDto;
import com.medicare.dto.BillResponseDto;
import com.medicare.dto.BillUpdateRequestDto;
import com.medicare.entity.Appointment;
import com.medicare.entity.AppointmentStatus;
import com.medicare.entity.Bill;
import com.medicare.entity.BillItem;
import com.medicare.entity.Patient;
import com.medicare.entity.PaymentStatus;
import com.medicare.entity.Role;
import com.medicare.exception.InvalidRequestException;
import com.medicare.exception.ResourceNotFoundException;
import com.medicare.mapper.EntityMapper;
import com.medicare.repository.AppointmentRepository;
import com.medicare.repository.BillRepository;
import com.medicare.repository.PatientRepository;
import com.medicare.security.CurrentUserService;
import com.medicare.service.BillService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * ACCESS MODEL
 *   ADMIN, RECEPTIONIST - create, read all, edit (while PENDING), and
 *                         settle (mark PAID / CANCELLED)
 *   PATIENT             - read-only, only their own bills
 *   DOCTOR              - no access (billing is a front-desk concern)
 *
 * MONEY RULES
 *   - The total is always computed here, never accepted from a client.
 *   - A bill can only be edited or settled while it is PENDING. PAID
 *     and CANCELLED are final: financial records must not be silently
 *     rewritten after the fact. (Refunds are out of scope.)
 *   - No delete. A wrong bill is CANCELLED, which keeps the history.
 */
@Service
@Transactional
public class BillServiceImpl implements BillService {

    // decimal(10,2) holds at most 99,999,999.99
    private static final BigDecimal MAX_TOTAL = new BigDecimal("99999999.99");

    private final BillRepository billRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final CurrentUserService currentUserService;
    private final EntityMapper mapper;

    public BillServiceImpl(BillRepository billRepository,
                           PatientRepository patientRepository,
                           AppointmentRepository appointmentRepository,
                           CurrentUserService currentUserService,
                           EntityMapper mapper) {
        this.billRepository = billRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.currentUserService = currentUserService;
        this.mapper = mapper;
    }

    @Override
    public List<BillResponseDto> getAllBills(Long patientId) {
        Role role = currentUserService.getCurrentRole();
        List<Bill> bills;

        if (isStaff(role)) {
            bills = (patientId == null)
                    ? billRepository.findAllByOrderByCreatedDateDescIdDesc()
                    : billRepository.findByPatientIdOrderByCreatedDateDescIdDesc(patientId);
        } else if (role == Role.PATIENT) {
            // A patient can never widen their view: any patientId they
            // pass is ignored and replaced by their own.
            Long ownPatientId = currentUserService.getCurrentPatientId();
            if (ownPatientId == null) {
                return List.of();
            }
            bills = billRepository.findByPatientIdOrderByCreatedDateDescIdDesc(ownPatientId);
        } else {
            throw new AccessDeniedException("Your role cannot access bills");
        }

        return bills.stream().map(this::toResponseDto).toList();
    }

    @Override
    public BillResponseDto getBillById(Long id) {
        Bill bill = findBillOrThrow(id);
        assertCanView(bill);
        return toResponseDto(bill);
    }

    @Override
    public BillResponseDto createBill(BillRequestDto dto) {
        assertStaff();

        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient not found with id: " + dto.getPatientId()));

        Bill bill = new Bill();
        bill.setPatient(patient);

        if (dto.getAppointmentId() != null) {
            Appointment appointment = appointmentRepository.findById(dto.getAppointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Appointment not found with id: " + dto.getAppointmentId()));

            if (!appointment.getPatient().getId().equals(patient.getId())) {
                throw new InvalidRequestException("This appointment does not belong to the selected patient");
            }
            if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                throw new InvalidRequestException("Cannot bill a cancelled appointment");
            }
            if (billRepository.existsByAppointmentIdAndPaymentStatusNot(
                    appointment.getId(), PaymentStatus.CANCELLED)) {
                throw new InvalidRequestException("This appointment already has a bill");
            }
            bill.setAppointment(appointment);
        }

        applyCharges(bill, dto);
        return toResponseDto(billRepository.save(bill));
    }

    @Override
    public BillResponseDto updateBill(Long id, BillUpdateRequestDto dto) {
        assertStaff();
        Bill bill = findBillOrThrow(id);

        if (bill.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new InvalidRequestException(
                    "Only PENDING bills can be edited - this bill is " + bill.getPaymentStatus());
        }

        applyCharges(bill, dto);
        return toResponseDto(billRepository.save(bill));
    }

    @Override
    public BillResponseDto updateBillStatus(Long id, PaymentStatus newStatus) {
        assertStaff();
        Bill bill = findBillOrThrow(id);

        if (bill.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new InvalidRequestException(
                    "This bill is already " + bill.getPaymentStatus() + " and can no longer change");
        }
        if (newStatus == PaymentStatus.PENDING) {
            throw new InvalidRequestException("Status can only be changed to PAID or CANCELLED");
        }

        bill.setPaymentStatus(newStatus);
        return toResponseDto(billRepository.save(bill));
    }

    // ---------------- helpers ----------------

    private boolean isStaff(Role role) {
        return role == Role.ADMIN || role == Role.RECEPTIONIST;
    }

    // Second safety net behind SecurityConfig, in case that config is
    // ever loosened by mistake.
    private void assertStaff() {
        if (!isStaff(currentUserService.getCurrentRole())) {
            throw new AccessDeniedException("Only ADMIN or RECEPTIONIST can manage bills");
        }
    }

    private void assertCanView(Bill bill) {
        Role role = currentUserService.getCurrentRole();
        if (isStaff(role)) {
            return;
        }
        if (role == Role.PATIENT) {
            Long ownPatientId = currentUserService.getCurrentPatientId();
            if (bill.getPatient().getId().equals(ownPatientId)) {
                return;
            }
            throw new AccessDeniedException("You can only access your own bills");
        }
        throw new AccessDeniedException("Your role cannot access bills");
    }

    private Bill findBillOrThrow(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));
    }

    // Money normaliser: null -> 0.00, and everything at 2 decimals so
    // amounts print consistently (500 becomes 500.00).
    private BigDecimal money(BigDecimal value) {
        BigDecimal v = (value == null) ? BigDecimal.ZERO : value;
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Sets the five charges and replaces the item list, then computes
     * the total. The total is worked out and VALIDATED first, and the
     * bill is only changed afterwards, so a rejected request never
     * leaves a half-modified bill behind.
     *
     * Items use clear() + add() on the SAME list (not setItems(new))
     * because Bill.items has orphanRemoval = true - see the same note
     * in PrescriptionServiceImpl.updatePrescription.
     */
    private void applyCharges(Bill bill, BillUpdateRequestDto dto) {
        BigDecimal consultation = money(dto.getConsultationCharge());
        BigDecimal lab = money(dto.getLabCharge());
        BigDecimal medicine = money(dto.getMedicineCharge());
        BigDecimal procedure = money(dto.getProcedureCharge());
        BigDecimal other = money(dto.getOtherCharge());

        BigDecimal total = consultation.add(lab).add(medicine).add(procedure).add(other);
        List<BillItemRequestDto> itemDtos = (dto.getItems() == null) ? List.of() : dto.getItems();
        for (BillItemRequestDto itemDto : itemDtos) {
            total = total.add(money(itemDto.getAmount()));
        }

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Bill total must be greater than zero");
        }
        if (total.compareTo(MAX_TOTAL) > 0) {
            throw new InvalidRequestException("Bill total is too large (maximum 99999999.99)");
        }

        bill.setConsultationCharge(consultation);
        bill.setLabCharge(lab);
        bill.setMedicineCharge(medicine);
        bill.setProcedureCharge(procedure);
        bill.setOtherCharge(other);

        bill.getItems().clear();
        for (BillItemRequestDto itemDto : itemDtos) {
            BillItem item = new BillItem();
            item.setDescription(itemDto.getDescription().trim());
            item.setAmount(money(itemDto.getAmount()));
            bill.addItem(item);
        }

        bill.setTotalAmount(total);
    }

    private BillResponseDto toResponseDto(Bill bill) {
        BillResponseDto dto = new BillResponseDto();
        dto.setId(bill.getId());
        dto.setCreatedDate(bill.getCreatedDate());
        if (bill.getAppointment() != null) {
            dto.setAppointmentId(bill.getAppointment().getId());
        }
        dto.setPatient(mapper.toPatientDto(bill.getPatient()));
        dto.setConsultationCharge(bill.getConsultationCharge());
        dto.setLabCharge(bill.getLabCharge());
        dto.setMedicineCharge(bill.getMedicineCharge());
        dto.setProcedureCharge(bill.getProcedureCharge());
        dto.setOtherCharge(bill.getOtherCharge());
        dto.setItems(bill.getItems().stream().map(this::toItemDto).toList());
        dto.setTotalAmount(bill.getTotalAmount());
        dto.setPaymentStatus(bill.getPaymentStatus());
        return dto;
    }

    private BillItemResponseDto toItemDto(BillItem item) {
        BillItemResponseDto dto = new BillItemResponseDto();
        dto.setId(item.getId());
        dto.setDescription(item.getDescription());
        dto.setAmount(item.getAmount());
        return dto;
    }
}
