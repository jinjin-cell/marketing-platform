package cn.qijiv.trigger.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthAttemptLimiterTest {

    @Test
    void failedLoginIsTemporarilyBlockedForAccountAndIp() {
        AtomicLong now = new AtomicLong(1_000L);
        AuthAttemptLimiter limiter = new AuthAttemptLimiter(now::get);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.allowLogin("unit_account", request));
            limiter.recordLoginFailure("unit_account", request);
        }
        assertFalse(limiter.allowLogin("unit_account", request));

        now.addAndGet(15L * 60L * 1000L);
        assertTrue(limiter.allowLogin("unit_account", request));
    }

    @Test
    void registrationIsLimitedPerIpAndRecoversAfterWindow() {
        AtomicLong now = new AtomicLong(1_000L);
        AuthAttemptLimiter limiter = new AuthAttemptLimiter(now::get);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "203.0.113.10");

        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.allowRegistration(request));
        }
        assertFalse(limiter.allowRegistration(request));

        now.addAndGet(60L * 60L * 1000L);
        assertTrue(limiter.allowRegistration(request));
    }
}
