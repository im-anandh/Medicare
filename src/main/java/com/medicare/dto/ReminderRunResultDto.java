package com.medicare.dto;

import java.time.LocalDateTime;

/** Summary of one reminder run (what the manual trigger endpoint returns). */
public class ReminderRunResultDto {

    // appointments found inside the reminder window that still needed a reminder
    private int checked;
    private int sent;
    private int failed;
    // found, but no longer eligible by the time they were processed,
    // or already failed the maximum number of times
    private int skipped;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private String note;

    public ReminderRunResultDto() {
    }

    public static ReminderRunResultDto busy() {
        ReminderRunResultDto dto = new ReminderRunResultDto();
        dto.note = "A reminder run is already in progress - nothing was started";
        return dto;
    }

    public int getChecked() {
        return checked;
    }

    public void setChecked(int checked) {
        this.checked = checked;
    }

    public int getSent() {
        return sent;
    }

    public void setSent(int sent) {
        this.sent = sent;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalDateTime windowStart) {
        this.windowStart = windowStart;
    }

    public LocalDateTime getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(LocalDateTime windowEnd) {
        this.windowEnd = windowEnd;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
