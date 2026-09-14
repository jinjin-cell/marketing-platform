package cn.qijiv.trigger.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JwtTokenServiceTest {

    @Test
    public void issuedTokenCanBeVerified() {
        JwtTokenService service = new JwtTokenService(
                "unit-test-jwt-secret-that-is-longer-than-32-chars", 300);

        String token = service.create("u_test", "unit_account");

        assertEquals("u_test", service.verifyAndGetUserId(token));
        assertEquals(300L, service.getExpiresInSeconds());
    }
}
