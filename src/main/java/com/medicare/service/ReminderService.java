package com.medicare.service;

import com.medicare.dto.NotificationResponseDto;
import com.medicare.dto.ReminderRunResultDto;

import java.util.List;

public interface ReminderService {

    /** Find appointments that are due a reminder and send them. Called by the scheduler and by the manual trigger. */
    ReminderRunResultDto runReminderCycle();

    /** The SMS delivery log, newest first, optionally for one appointment. */
    List<NotificationResponseDto> getNotificationLog(Long appointmentId);
}
