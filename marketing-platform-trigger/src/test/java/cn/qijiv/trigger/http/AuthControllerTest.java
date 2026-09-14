package cn.qijiv.trigger.http;

import cn.qijiv.infrastructure.dao.IUserAccountDao;
import cn.qijiv.trigger.api.dto.AuthRequestDTO;
import cn.qijiv.trigger.api.dto.AuthResponseDTO;
import cn.qijiv.trigger.security.AuthAttemptLimiter;
import cn.qijiv.trigger.security.JwtTokenService;
import cn.qijiv.trigger.security.PasswordHasher;
import cn.qijiv.trigger.security.TokenSessionService;
import cn.qijiv.types.model.Response;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void sixCharacterPasswordCanRegisterAndCreatesRedisSession() {
        IUserAccountDao userAccountDao = mock(IUserAccountDao.class);
        AuthAttemptLimiter limiter = mock(AuthAttemptLimiter.class);
        TokenSessionService tokenSessionService = mock(TokenSessionService.class);
        when(limiter.allowRegistration(any())).thenReturn(true);
        when(userAccountDao.insert(any())).thenReturn(1);
        AuthController controller = new AuthController(
                userAccountDao,
                new PasswordHasher(),
                new JwtTokenService("unit-test-jwt-secret-that-is-longer-than-32-chars", 604800),
                tokenSessionService,
                limiter);
        AuthRequestDTO request = new AuthRequestDTO();
        request.setAccountName("qijiv_test");
        request.setPassword("123456");

        Response<AuthResponseDTO> response = controller.register(request, new MockHttpServletRequest());

        assertEquals("0000", response.getCode());
        assertNotNull(response.getData().getAccessToken());
        assertEquals(604800L, response.getData().getExpiresInSeconds());
        verify(tokenSessionService).save(
                org.mockito.ArgumentMatchers.eq(response.getData().getUserId()),
                org.mockito.ArgumentMatchers.eq(response.getData().getAccessToken()));
    }
}
