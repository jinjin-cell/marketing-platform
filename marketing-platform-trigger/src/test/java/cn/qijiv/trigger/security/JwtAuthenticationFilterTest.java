package cn.qijiv.trigger.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {
    private final JwtTokenService tokenService = new JwtTokenService(
            "unit-test-jwt-secret-that-is-longer-than-32-chars", 300);
    private final TokenSessionService tokenSessionService = mock(TokenSessionService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, tokenSessionService);

    @Test
    void missingTokenReturnsHttp401WithBusinessCode() throws Exception {
        MockHttpServletRequest request = businessRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("unauthenticated request must not reach the application");
        });

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":\"0401\""));
        assertNull(AuthenticatedUser.current());
    }

    @Test
    void validTokenSuppliesIdentityAndAlwaysClearsThreadLocal() throws Exception {
        MockHttpServletRequest request = businessRequest();
        String token = tokenService.create("u_token", "unit_account");
        when(tokenSessionService.isActive("u_token", token)).thenReturn(true);
        request.addHeader("Authorization", "bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> observed = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> observed.set(AuthenticatedUser.current()));

        assertEquals("u_token", observed.get());
        assertNull(AuthenticatedUser.current());
    }

    @Test
    void downstreamFailureIsNotMisreportedAsAuthenticationFailure() {
        MockHttpServletRequest request = businessRequest();
        String token = tokenService.create("u_token", "unit_account");
        when(tokenSessionService.isActive("u_token", token)).thenReturn(true);
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain failingChain = (req, res) -> {
            throw new ServletException("downstream failure");
        };

        ServletException error = assertThrows(ServletException.class,
                () -> filter.doFilter(request, response, failingChain));

        assertEquals("downstream failure", error.getMessage());
        assertNull(AuthenticatedUser.current());
    }

    @Test
    void tokenMissingFromRedisReturnsUnauthorized() throws Exception {
        MockHttpServletRequest request = businessRequest();
        String token = tokenService.create("u_token", "unit_account");
        when(tokenSessionService.isActive("u_token", token)).thenReturn(false);
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("revoked session must not reach the application");
        });

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":\"0401\""));
        assertNull(AuthenticatedUser.current());
    }

    private MockHttpServletRequest businessRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/raffle/activity/query_user_credit");
        return request;
    }
}
