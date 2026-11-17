package com.medicare.service.impl;

import com.medicare.dto.NotificationResponseDto;
import com.medicare.dto.ReminderRunResultDto;
import com.medicare.entity.Appointment;
import com.medicare.entity.AppointmentStatus;
import com.medicare.entity.Notification;
import com.medicare.entity.NotificationStatus;
import com.medicare.integration.sms.SmsResult;
import com.medicare.integration.sms.SmsService;
import com.medicare.repository.AppointmentRepository;
import com.medicare.repository.NotificationRepository;
import com.medicare.service.ReminderService;
import com.medicare.util.PhoneNumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RULES
 *  - Only SCHEDULED appointments are reminded (cancelled, completed and
 *    missed ones never are).
 *  - A reminder is due when the appointment starts within the next
 *    reminder.lead-hours hours (default 24) and has not been reminded.
 *  - Every attempt, successful or not, is written to the notifications
 *    table. Appointment.reminderSent only becomes true on SUCCESS, so a
 *    failed send is retried on the next run - but at most
 *    reminder.max-attempts times, so a bad number can't be retried forever.
 *  - Rescheduling an appointment clears reminderSent (see
 *    AppointmentServiceImpl), so the new time gets its own reminder.
 *
 * WHY EACH APPOINTMENT GETS ITS OWN TRANSACTION
 *  Sending an SMS cannot be undone by a database rollback. If one big
 *  transaction covered every appointment and something failed near the
 *  end, the database would roll back the "reminder sent" records of
 *  SMS messages that were ALREADY delivered, and the next run would
 *  text those patients a second time. So we commit after each
 *  appointment: a problem with one can never undo the record of another.
 *  (The remaining small window - the app crashing between sending and
 *  recording - can only ever cause a duplicate, never a missed reminder.)
 *
 * The transactions are programmatic (TransactionTemplate) rather than
 * @Transactional, so that this commit-per-appointment behaviour is
 * explicit and cannot be defeated by calling one method from another.
 */
@Service
public class ReminderServiceImpl implements ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH);
    private static final int MAX_FAILURE_REASON_LENGTH = 255;   // notifications.failure_reason is varchar(255)
    private static final int MAX_MESSAGE_LENGTH = 500;          // notifications.message is length 500

    private enum Outcome { SENT, FAILED, SKIPPED }

    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final SmsService smsService;
    private final Clock clock;
    private final TransactionTemplate readTx;
    private final TransactionTemplate writeTx;
    private final int leadHours;
    private final int maxAttempts;
    private final String defaultCountryCode;

    // Prevents the scheduler and a manual trigger from running at the
    // same moment and texting the same patient twice.
    private final ReentrantLock runLock = new ReentrantLock();

    public ReminderServiceImpl(AppointmentRepository appointmentRepository,
                               NotificationRepository notificationRepository,
                               SmsService smsService,
                               Clock clock,
                               PlatformTransactionManager transactionManager,
                               @Value("${reminder.lead-hours:24}") int leadHours,
                               @Value("${reminder.max-attempts:3}") int maxAttempts,
                               @Value("${sms.default-country-code:+91}") String defaultCountryCode) {
        if (leadHours < 1 || maxAttempts < 1) {
            throw new IllegalStateException("reminder.lead-hours and reminder.max-attempts must both be at least 1");
        }
        this.appointmentRepository = appointmentRepository;
        this.notificationRepository = notificationRepository;
        this.smsService = smsService;
        this.clock = clock;
        this.leadHours = leadHours;
        this.maxAttempts = maxAttempts;
        this.defaultCountryCode = defaultCountryCode;

        this.readTx = new TransactionTemplate(transactionManager);
        this.readTx.setReadOnly(true);
        this.writeTx = new TransactionTemplate(transactionManager);
    }

    @Override
    public ReminderRunResultDto runReminderCycle() {
        if (!runLock.tryLock()) {
            log.info("Reminder run skipped: another run is already in progress");
            return ReminderRunResultDto.busy();
        }
        try {
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime windowEnd = now.plusHours(leadHours);

            // Step 1: find the candidates. Only their IDs are kept - each
            // one is re-loaded fresh in its own transaction below.
            List<Long> candidateIds = readTx.execute(status ->
                    appointmentRepository
                            .findByStatusAndReminderSentFalseAndAppointmentDateBetween(
                                    AppointmentStatus.SCHEDULED, now.toLocalDate(), windowEnd.toLocalDate())
                            .stream()
                            .filter(a -> isInWindow(a, now, windowEnd))
                            .map(Appointment::getId)
                            .toList());
            if (candidateIds == null) {
                candidateIds = List.of();
            }

            // Step 2: process each candidate independently.
            int sent = 0, failed = 0, skipped = 0;
            for (Long id : candidateIds) {
                Outcome outcome;
                try {
                    outcome = writeTx.execute(status -> processOne(id, now, windowEnd));
                } catch (RuntimeException e) {
                    // e.g. a database error: that appointment's transaction
                    // rolled back, the others are unaffected.
                    log.error("Reminder processing failed for appointment {}", id, e);
                    outcome = Outcome.FAILED;
                }
                switch (outcome) {
                    case SENT -> sent++;
                    case FAILED -> failed++;
                    default -> skipped++;
                }
            }

            ReminderRunResultDto result = new ReminderRunResultDto();
            result.setChecked(candidateIds.size());
            result.setSent(sent);
            result.setFailed(failed);
            result.setSkipped(skipped);
            result.setWindowStart(now);
            result.setWindowEnd(windowEnd);

            if (!candidateIds.isEmpty()) {
                log.info("Reminder run finished: checked={}, sent={}, failed={}, skipped={}",
                        candidateIds.size(), sent, failed, skipped);
            }
            return result;
        } finally {
            runLock.unlock();
        }
    }

    // Runs inside its own transaction (see runReminderCycle).
    private Outcome processOne(Long appointmentId, LocalDateTime now, LocalDateTime windowEnd) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        // Re-check everything: the appointment may have been cancelled,
        // rescheduled or already reminded since the candidate list was built.
        if (appointment == null
                || appointment.getStatus() != AppointmentStatus.SCHEDULED
                || appointment.isReminderSent()
                || !isInWindow(appointment, now, windowEnd)) {
            return Outcome.SKIPPED;
        }

        long failuresSoFar = notificationRepository
                .countByAppointmentIdAndStatus(appointmentId, NotificationStatus.FAILED);
        if (failuresSoFar >= maxAttempts) {
            log.warn("Giving up on reminder for appointment {} after {} failed attempts", appointmentId, failuresSoFar);
            return Outcome.SKIPPED;
        }

        String message = buildMessage(appointment);
        Notification notification = new Notification();
        notification.setAppointment(appointment);
        notification.setMessage(truncate(message, MAX_MESSAGE_LENGTH));
        notification.setAttemptedAt(now);

        String phone = PhoneNumberUtil.normalize(appointment.getPatient().getPhone(), defaultCountryCode);
        if (phone == null) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason("Patient phone number is missing or not a valid number");
            notificationRepository.save(notification);
            return Outcome.FAILED;
        }

        SmsResult result;
        try {
            result = smsService.send(phone, message);
        } catch (RuntimeException e) {
            // Providers are told not to throw, but a bug in one must not
            // stop the other reminders.
            result = SmsResult.failure("Unexpected error from SMS provider: " + e.getMessage());
        }

        if (result.success()) {
            notification.setStatus(NotificationStatus.SENT);
            notificationRepository.save(notification);
            appointment.setReminderSent(true);
            appointment.setReminderSentAt(now);
            appointmentRepository.save(appointment);
            return Outcome.SENT;
        }

        notification.setStatus(NotificationStatus.FAILED);
        notification.setFailureReason(truncate(result.error(), MAX_FAILURE_REASON_LENGTH));
        notificationRepository.save(notification);
        return Outcome.FAILED;
    }

    @Override
    public List<NotificationResponseDto> getNotificationLog(Long appointmentId) {
        List<NotificationResponseDto> entries = readTx.execute(status -> {
            List<Notification> rows = (appointmentId == null)
                    ? notificationRepository.findAllByOrderByAttemptedAtDescIdDesc()
                    : notificationRepository.findByAppointmentIdOrderByAttemptedAtDescIdDesc(appointmentId);
            return rows.stream().map(this::toDto).toList();
        });
        return entries == null ? List.of() : entries;
    }

    // ---------------- helpers ----------------

    // Appointment dates and times have no time zone stored; they are
    // read in the zone the application clock uses (app.timezone).
    private boolean isInWindow(Appointment a, LocalDateTime now, LocalDateTime windowEnd) {
        LocalDateTime start = LocalDateTime.of(a.getAppointmentDate(), a.getAppointmentTime());
        return !start.isBefore(now) && !start.isAfter(windowEnd);
    }

    // Plain ASCII on purpose: a single non-ASCII character (some JDKs
    // put a special space before "AM"/"PM") would force the SMS into a
    // costlier encoding that splits into more segments.
    private String buildMessage(Appointment a) {
        String fullName = a.getPatient().getFullName() == null ? "" : a.getPatient().getFullName().trim();
        String firstName = fullName.isEmpty() ? "there" : fullName.split("\\s+")[0];
        return "MediCare reminder: Hi " + firstName + ", you have an appointment with "
                + a.getDoctor().getName() + " on " + DATE_FORMAT.format(a.getAppointmentDate())
                + " at " + formatTime(a.getAppointmentTime().getHour(), a.getAppointmentTime().getMinute())
                + ". Please arrive 10 minutes early.";
    }

    private String formatTime(int hour24, int minute) {
        int hour12 = hour24 % 12 == 0 ? 12 : hour24 % 12;
        return String.format("%d:%02d %s", hour12, minute, hour24 < 12 ? "AM" : "PM");
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }

    private NotificationResponseDto toDto(Notification n) {
        Appointment a = n.getAppointment();
        NotificationResponseDto dto = new NotificationResponseDto();
        dto.setId(n.getId());
        dto.setAppointmentId(a.getId());
        dto.setPatientId(a.getPatient().getId());
        dto.setPatientName(a.getPatient().getFullName());
        dto.setDoctorName(a.getDoctor().getName());
        dto.setAppointmentDate(a.getAppointmentDate());
        dto.setAppointmentTime(a.getAppointmentTime());
        dto.setMessage(n.getMessage());
        dto.setStatus(n.getStatus());
        dto.setAttemptedAt(n.getAttemptedAt());
        dto.setFailureReason(n.getFailureReason());
        return dto;
    }
}
