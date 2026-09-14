package cn.qijiv.trigger.security;

import cn.qijiv.infrastructure.redis.IRedisService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenSessionServiceTest {

    @Test
    void saveStoresDigestForExactlyOneWeek() {
        IRedisService redisService = mock(IRedisService.class);
        TokenSessionService service = new TokenSessionService(redisService, 604800);

        service.save("u_test", "signed.jwt.value");

        verify(redisService).setValue(
                org.mockito.ArgumentMatchers.eq("big_market_auth_token_u_test"),
                anyString(),
                org.mockito.ArgumentMatchers.eq(604800L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS));
    }

    @Test
    void activeSessionRequiresMatchingTokenDigest() {
        IRedisService redisService = mock(IRedisService.class);
        TokenSessionService service = new TokenSessionService(redisService, 604800);
        service.save("u_test", "current-token");
        org.mockito.ArgumentCaptor<String> digest = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(redisService).setValue(
                org.mockito.ArgumentMatchers.eq("big_market_auth_token_u_test"), digest.capture(),
                org.mockito.ArgumentMatchers.eq(604800L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS));
        when(redisService.getValue("big_market_auth_token_u_test")).thenReturn(digest.getValue());

        assertTrue(service.isActive("u_test", "current-token"));
        assertFalse(service.isActive("u_test", "previous-token"));
    }
}
