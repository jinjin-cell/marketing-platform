package cn.qijiv.trigger.http;

import cn.qijiv.infrastructure.dao.IUserAccountDao;
import cn.qijiv.infrastructure.dao.po.UserAccountPO;
import cn.qijiv.trigger.api.dto.AuthRequestDTO;
import cn.qijiv.trigger.api.dto.AuthResponseDTO;
import cn.qijiv.trigger.security.AuthAttemptLimiter;
import cn.qijiv.trigger.security.JwtTokenService;
import cn.qijiv.trigger.security.PasswordHasher;
import cn.qijiv.trigger.security.TokenSessionService;
import cn.qijiv.types.enums.ResponseCode;
import cn.qijiv.types.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/api/${app.config.api-version}/auth/")
public class AuthController {
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("[A-Za-z0-9_]{4,32}");
    private final SecureRandom secureRandom = new SecureRandom();
    private final IUserAccountDao userAccountDao;
    private final PasswordHasher passwordHasher;
    private final JwtTokenService jwtTokenService;
    private final TokenSessionService tokenSessionService;
    private final AuthAttemptLimiter authAttemptLimiter;
    private final PasswordHasher.PasswordHash dummyPasswordHash;

    public AuthController(IUserAccountDao userAccountDao, PasswordHasher passwordHasher,
                          JwtTokenService jwtTokenService, TokenSessionService tokenSessionService,
                          AuthAttemptLimiter authAttemptLimiter) {
        this.userAccountDao = userAccountDao;
        this.passwordHasher = passwordHasher;
        this.jwtTokenService = jwtTokenService;
        this.tokenSessionService = tokenSessionService;
        this.authAttemptLimiter = authAttemptLimiter;
        this.dummyPasswordHash = passwordHasher.hash(newUserId());
    }

    @PostMapping("register")
    public Response<AuthResponseDTO> register(@RequestBody AuthRequestDTO request,
                                              HttpServletRequest httpRequest) {
        String accountName = normalizeAccount(request);
        String validationError = validate(accountName, request == null ? null : request.getPassword());
        if (validationError != null) return failure(ResponseCode.ILLEGAL_PARAMETER, validationError);
        if (!authAttemptLimiter.allowRegistration(httpRequest)) {
            return failure(ResponseCode.RATE_LIMITER, "注册请求过于频繁，请稍后再试");
        }

        PasswordHasher.PasswordHash passwordHash = passwordHasher.hash(request.getPassword());
        String userId = newUserId();
        try {
            userAccountDao.insert(UserAccountPO.builder()
                    .accountName(accountName)
                    .userId(userId)
                    .passwordHash(passwordHash.getHash())
                    .passwordSalt(passwordHash.getSalt())
                    .accountStatus("open")
                    .build());
            return success(accountName, userId);
        } catch (DuplicateKeyException ex) {
            log.info("账号注册冲突 accountName:{}", accountName);
            return failure(ResponseCode.INDEX_DUP, "账号已存在");
        }
    }

    @PostMapping("login")
    public Response<AuthResponseDTO> login(@RequestBody AuthRequestDTO request,
                                           HttpServletRequest httpRequest) {
        String accountName = normalizeAccount(request);
        if (StringUtils.isBlank(accountName) || request == null || StringUtils.isBlank(request.getPassword())) {
            return failure(ResponseCode.ILLEGAL_PARAMETER, "请输入账号和密码");
        }
        if (!authAttemptLimiter.allowLogin(accountName, httpRequest)) {
            return failure(ResponseCode.RATE_LIMITER, "登录尝试次数过多，请稍后再试");
        }
        UserAccountPO account = userAccountDao.queryByAccountName(accountName);
        String storedHash = account == null ? dummyPasswordHash.getHash() : account.getPasswordHash();
        String storedSalt = account == null ? dummyPasswordHash.getSalt() : account.getPasswordSalt();
        boolean passwordMatches = passwordHasher.matches(request.getPassword(), storedHash, storedSalt);
        if (account == null || !"open".equals(account.getAccountStatus()) || !passwordMatches) {
            authAttemptLimiter.recordLoginFailure(accountName, httpRequest);
            return failure(ResponseCode.UNAUTHORIZED, "账号或密码错误");
        }
        authAttemptLimiter.recordLoginSuccess(accountName, httpRequest);
        return success(account.getAccountName(), account.getUserId());
    }

    private Response<AuthResponseDTO> success(String accountName, String userId) {
        String accessToken = jwtTokenService.create(userId, accountName);
        tokenSessionService.save(userId, accessToken);
        return Response.<AuthResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(AuthResponseDTO.builder()
                        .accountName(accountName)
                        .userId(userId)
                        .accessToken(accessToken)
                        .expiresInSeconds(jwtTokenService.getExpiresInSeconds())
                        .build())
                .build();
    }

    private Response<AuthResponseDTO> failure(ResponseCode code, String message) {
        return Response.<AuthResponseDTO>builder().code(code.getCode()).info(message).build();
    }

    private String normalizeAccount(AuthRequestDTO request) {
        return request == null ? null : StringUtils.trimToEmpty(request.getAccountName()).toLowerCase(Locale.ROOT);
    }

    private String validate(String accountName, String password) {
        if (!ACCOUNT_PATTERN.matcher(StringUtils.defaultString(accountName)).matches()) {
            return "账号仅支持 4-32 位字母、数字或下划线";
        }
        if (password == null || password.length() < 6 || password.length() > 72) {
            return "密码需为 6-72 位";
        }
        return null;
    }

    private String newUserId() {
        byte[] bytes = new byte[15];
        secureRandom.nextBytes(bytes);
        return "u_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
