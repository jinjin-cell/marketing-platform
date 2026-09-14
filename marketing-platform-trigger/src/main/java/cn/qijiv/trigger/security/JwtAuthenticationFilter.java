package cn.qijiv.trigger.security;

import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.model.Response;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;
    private final TokenSessionService tokenSessionService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   TokenSessionService tokenSessionService) {
        this.jwtTokenService = jwtTokenService;
        this.tokenSessionService = tokenSessionService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !uri.contains("/api/")
                || uri.contains("/auth/")
                || uri.contains("/raffle/admin/")
                || uri.contains("/raffle/dcc/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.isBlank(authorization)
                || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            unauthorized(response);
            return;
        }
        String token = authorization.substring(7).trim();
        String userId;
        try {
            userId = jwtTokenService.verifyAndGetUserId(token);
            if (StringUtils.isBlank(userId) || !tokenSessionService.isActive(userId, token)) {
                unauthorized(response);
                return;
            }
        } catch (Exception ex) {
            unauthorized(response);
            return;
        }

        AuthenticatedUser.set(userId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuthenticatedUser.clear();
        }
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JSON.toJSONString(Response.builder()
                .code(ResponseCode.UNAUTHORIZED.getCode())
                .info(ResponseCode.UNAUTHORIZED.getInfo())
                .build()));
    }
}
