package com.medicare.scheduler;

import com.medicare.dto.ReminderRunResultDto;
import com.medicare.service.ReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Wakes up on a timer and asks ReminderService to send whatever
 * reminders are due. It contains no reminder logic of its own - the
 * same ReminderService method is also behind the manual trigger
 * endpoint, so what you test by hand is exactly what the timer runs.
 *
 * reminder.cron is a Spring cron expression with SIX fields:
 *   second minute hour day-of-month month day-of-week
 * The default "0 0/15 * * * *" means "at second 0, every 15 minutes".
 * Set reminder.enabled=false (REMINDER_ENABLED=false) to turn the
 * timer off completely; the manual endpoint still works.
 */
@Component
@ConditionalOnProperty(name = "reminder.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderService reminderService;

    public ReminderScheduler(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(cron = "${reminder.cron:0 0/15 * * * *}", zone = "${app.timezone:Asia/Kolkata}")
    public void sendDueReminders() {
        try {
            ReminderRunResultDto result = reminderService.runReminderCycle();
            log.debug("Scheduled reminder run: checked={}, sent={}, failed={}, skipped={}",
                    result.getChecked(), result.getSent(), result.getFailed(), result.getSkipped());
        } catch (RuntimeException e) {
            // An exception escaping a @Scheduled method is only logged by
            // Spring, but catching it here keeps the message clear.
            log.error("Scheduled reminder run crashed", e);
        }
    }
}
