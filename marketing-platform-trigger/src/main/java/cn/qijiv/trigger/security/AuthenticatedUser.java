package cn.qijiv.trigger.security;

import org.apache.commons.lang3.StringUtils;

public final class AuthenticatedUser {
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    private AuthenticatedUser() {
    }

    public static void set(String userId) {
        USER_ID.set(userId);
    }

    public static String current() {
        return USER_ID.get();
    }

    /** HTTP 请求始终使用 JWT 身份；非 HTTP/Dubbo 调用保持原有显式 userId。 */
    public static String resolve(String requestedUserId) {
        String authenticated = USER_ID.get();
        return StringUtils.isBlank(authenticated) ? requestedUserId : authenticated;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
