package com.medicare.integration.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The default provider: sends NOTHING, just prints the SMS to the
 * application log. It lets the entire reminder flow (scheduling,
 * logging, retries, status flags) be developed and demonstrated with
 * no account, no cost and no real phone being messaged.
 *
 * simulateFailure = true makes every send fail on purpose, so the
 * FAILED-notification and retry behaviour can be demonstrated too
 * (set SMS_MOCK_FAIL=true).
 */
public class MockSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(MockSmsService.class);

    private final boolean simulateFailure;
    private final AtomicLong counter = new AtomicLong();

    public MockSmsService(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public SmsResult send(String toPhone, String message) {
        if (simulateFailure) {
            log.warn("[MOCK SMS] simulated FAILURE for {}", toPhone);
            return SmsResult.failure("Simulated failure (sms.mock.simulate-failure=true)");
        }
        log.info("[MOCK SMS] to={} | {}", toPhone, message);
        return SmsResult.success("mock-" + counter.incrementAndGet());
    }
}
