package cn.qijiv.trigger.security;

import cn.qijiv.infrastructure.redis.IRedisService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/** Redis-backed single-session registry for issued access tokens. */
@Component
public class TokenSessionService {
    static final String SESSION_KEY_PREFIX = "big_market_auth_token_";

    private final IRedisService redisService;
    private final long expiresInSeconds;

    public TokenSessionService(IRedisService redisService,
                               @Value("${app.config.jwt-expires-seconds:604800}") long expiresInSeconds) {
        if (expiresInSeconds <= 0) {
            throw new IllegalStateException("Token 有效期必须大于 0 秒");
        }
        this.redisService = redisService;
        this.expiresInSeconds = expiresInSeconds;
    }

    public void save(String userId, String token) {
        if (StringUtils.isAnyBlank(userId, token)) {
            throw new IllegalArgumentException("用户 ID 和 Token 不能为空");
        }
        redisService.setValue(key(userId), digest(token), expiresInSeconds, TimeUnit.SECONDS);
    }

    public boolean isActive(String userId, String token) {
        if (StringUtils.isAnyBlank(userId, token)) return false;
        String storedDigest = redisService.getValue(key(userId));
        if (StringUtils.isBlank(storedDigest)) return false;
        return MessageDigest.isEqual(
                storedDigest.getBytes(StandardCharsets.UTF_8),
                digest(token).getBytes(StandardCharsets.UTF_8));
    }

    private String key(String userId) {
        return SESSION_KEY_PREFIX + userId;
    }

    private String digest(String token) {
        try {
            byte[] value = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }
}
