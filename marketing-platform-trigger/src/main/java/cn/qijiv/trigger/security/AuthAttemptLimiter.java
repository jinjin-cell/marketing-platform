package cn.qijiv.trigger.security;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/** 登录与注册入口限流，避免密码哈希被高频请求耗尽 CPU。 */
@Component
public class AuthAttemptLimiter {
    private static final long MINUTE_MILLIS = 60_000L;
    private static final long FIFTEEN_MINUTES_MILLIS = 15L * MINUTE_MILLIS;
    private static final long HOUR_MILLIS = 60L * MINUTE_MILLIS;
    private static final int LOGIN_REQUESTS_PER_MINUTE = 30;
    private static final int LOGIN_FAILURES_PER_ACCOUNT_AND_IP = 5;
    private static final int REGISTRATIONS_PER_HOUR = 10;

    private final Map<String, WindowCounter> loginRequests = new ConcurrentHashMap<>();
    private final Map<String, WindowCounter> loginFailures = new ConcurrentHashMap<>();
    private final Map<String, WindowCounter> registrations = new ConcurrentHashMap<>();
    private final AtomicLong operationCount = new AtomicLong();
    private final LongSupplier clock;

    public AuthAttemptLimiter() {
        this(System::currentTimeMillis);
    }

    AuthAttemptLimiter(LongSupplier clock) {
        this.clock = clock;
    }

    public boolean allowLogin(String accountName, HttpServletRequest request) {
        long now = clock.getAsLong();
        cleanupPeriodically(now);
        String source = clientAddress(request);
        if (!incrementAndAllow(loginRequests, source, LOGIN_REQUESTS_PER_MINUTE, MINUTE_MILLIS, now)) {
            return false;
        }
        WindowCounter failures = loginFailures.get(loginFailureKey(accountName, source));
        return failures == null || failures.isExpired(now, FIFTEEN_MINUTES_MILLIS)
                || failures.count < LOGIN_FAILURES_PER_ACCOUNT_AND_IP;
    }

    public void recordLoginFailure(String accountName, HttpServletRequest request) {
        increment(loginFailures, loginFailureKey(accountName, clientAddress(request)),
                FIFTEEN_MINUTES_MILLIS, clock.getAsLong());
    }

    public void recordLoginSuccess(String accountName, HttpServletRequest request) {
        loginFailures.remove(loginFailureKey(accountName, clientAddress(request)));
    }

    public boolean allowRegistration(HttpServletRequest request) {
        long now = clock.getAsLong();
        cleanupPeriodically(now);
        return incrementAndAllow(registrations, clientAddress(request),
                REGISTRATIONS_PER_HOUR, HOUR_MILLIS, now);
    }

    private String loginFailureKey(String accountName, String source) {
        return StringUtils.defaultString(accountName) + '|' + source;
    }

    private boolean incrementAndAllow(Map<String, WindowCounter> counters, String key,
                                      int limit, long windowMillis, long now) {
        AtomicBoolean allowed = new AtomicBoolean(false);
        counters.compute(key, (ignored, current) -> {
            if (current == null || current.isExpired(now, windowMillis)) {
                allowed.set(true);
                return new WindowCounter(now, 1);
            }
            if (current.count >= limit) {
                return current;
            }
            allowed.set(true);
            return new WindowCounter(current.startedAt, current.count + 1);
        });
        return allowed.get();
    }

    private void increment(Map<String, WindowCounter> counters, String key,
                           long windowMillis, long now) {
        counters.compute(key, (ignored, current) ->
                current == null || current.isExpired(now, windowMillis)
                        ? new WindowCounter(now, 1)
                        : new WindowCounter(current.startedAt, current.count + 1));
    }

    private String clientAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String realIp = StringUtils.trimToNull(request.getHeader("X-Real-IP"));
        String address = realIp == null ? StringUtils.trimToNull(request.getRemoteAddr()) : realIp;
        if (address == null) {
            return "unknown";
        }
        return address.length() <= 64 ? address : address.substring(0, 64);
    }

    private void cleanupPeriodically(long now) {
        if ((operationCount.incrementAndGet() & 255L) != 0L) {
            return;
        }
        removeExpired(loginRequests, now, MINUTE_MILLIS);
        removeExpired(loginFailures, now, FIFTEEN_MINUTES_MILLIS);
        removeExpired(registrations, now, HOUR_MILLIS);
    }

    private void removeExpired(Map<String, WindowCounter> counters, long now, long windowMillis) {
        counters.entrySet().removeIf(entry -> entry.getValue().isExpired(now, windowMillis));
    }

    private static final class WindowCounter {
        private final long startedAt;
        private final int count;

        private WindowCounter(long startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }

        private boolean isExpired(long now, long windowMillis) {
            return now - startedAt >= windowMillis;
        }
    }
}
