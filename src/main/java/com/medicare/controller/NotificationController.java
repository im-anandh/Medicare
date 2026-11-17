package com.medicare.controller;

import com.medicare.dto.NotificationResponseDto;
import com.medicare.dto.ReminderRunResultDto;
import com.medicare.integration.sms.SmsService;
import com.medicare.service.ReminderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final ReminderService reminderService;
    private final SmsService smsService;

    public NotificationController(ReminderService reminderService, SmsService smsService) {
        this.reminderService = reminderService;
        this.smsService = smsService;
    }

    // Which SMS provider is active. "delivers": false means MOCK mode - messages
    // are only written to the server log, never sent to a phone.
    @GetMapping("/provider")
    public ResponseEntity<Map<String, Object>> getProvider() {
        String name = smsService.providerName();
        return ResponseEntity.ok(Map.of("provider", name, "delivers", !"mock".equals(name)));
    }

    // The SMS delivery log (ADMIN, RECEPTIONIST), newest first.
    // Optional ?appointmentId= narrows it to one appointment.
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(
            @RequestParam(required = false) Long appointmentId) {
        return ResponseEntity.ok(reminderService.getNotificationLog(appointmentId));
    }

    // Run a reminder cycle right now instead of waiting for the timer
    // (ADMIN only) - for testing and demos. It does exactly what the
    // scheduler does.
    @PostMapping("/run-reminders")
    public ResponseEntity<ReminderRunResultDto> runRemindersNow() {
        return ResponseEntity.ok(reminderService.runReminderCycle());
    }
}
