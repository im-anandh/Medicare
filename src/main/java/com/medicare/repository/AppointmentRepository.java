package com.medicare.repository;

import com.medicare.entity.Appointment;
import com.medicare.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Used when BOOKING a new appointment: "does the doctor already
    // have a SCHEDULED appointment at this exact date and time?"
    // existsBy... is more efficient than findBy... here - it asks the
    // database for a simple true/false instead of loading a full row
    // we don't actually need.
    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatus(
            Long doctorId, LocalDate appointmentDate, LocalTime appointmentTime, AppointmentStatus status);

    // Used when RESCHEDULING: same check, but excluding the
    // appointment being rescheduled itself (otherwise an appointment
    // would always "conflict" with its own current slot).
    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusAndIdNot(
            Long doctorId, LocalDate appointmentDate, LocalTime appointmentTime,
            AppointmentStatus status, Long excludedId);

    // Used by the reminder run: SCHEDULED appointments in a date range
    // that have not been reminded yet. (The exact time-of-day cut-off is
    // applied afterwards in Java - this just narrows it to a few days.)
    List<Appointment> findByStatusAndReminderSentFalseAndAppointmentDateBetween(
            AppointmentStatus status, LocalDate from, LocalDate to);

    List<Appointment> findByDoctorId(Long doctorId);

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, LocalDate appointmentDate);

    /**
     * WHAT DEFINES "this doctor's patients"?
     * We define it as: any patient who has ever had at least one
     * appointment with this doctor. This is a reasonable, simple rule
     * for a learning project - a doctor can see the profile of anyone
     * they've actually had a booked appointment with, past or future,
     * regardless of that appointment's status.
     *
     * @Query lets us write JPQL directly when a derived method name
     * (like findByDoctorId...) can't express what we need - here, we
     * want DISTINCT patient IDs, not a list of appointments. JPQL
     * looks like SQL but queries our ENTITY model (Appointment,
     * patient.id) rather than actual table/column names.
     */
    @Query("SELECT DISTINCT a.patient.id FROM Appointment a WHERE a.doctor.id = :doctorId")
    List<Long> findDistinctPatientIdsByDoctorId(@Param("doctorId") Long doctorId);
}
