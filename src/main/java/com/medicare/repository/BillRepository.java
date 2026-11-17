package com.medicare.repository;

import com.medicare.entity.Bill;
import com.medicare.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * All queries are derived from the method names (no SQL):
 *
 *   existsByAppointmentIdAndPaymentStatusNot(id, CANCELLED)
 *     -> "is there any bill for this appointment whose status is NOT
 *         cancelled?"  Used to stop the same appointment being billed
 *         twice, while still allowing a re-bill after a cancellation.
 */
public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findAllByOrderByCreatedDateDescIdDesc();

    List<Bill> findByPatientIdOrderByCreatedDateDescIdDesc(Long patientId);

    boolean existsByAppointmentIdAndPaymentStatusNot(Long appointmentId, PaymentStatus status);
}
