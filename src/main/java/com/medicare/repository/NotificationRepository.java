package com.medicare.repository;

import com.medicare.entity.Notification;
import com.medicare.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderByAttemptedAtDescIdDesc();

    List<Notification> findByAppointmentIdOrderByAttemptedAtDescIdDesc(Long appointmentId);

    // Used to stop retrying forever: "how many times has sending a
    // reminder for this appointment already FAILED?"
    long countByAppointmentIdAndStatus(Long appointmentId, NotificationStatus status);
}
