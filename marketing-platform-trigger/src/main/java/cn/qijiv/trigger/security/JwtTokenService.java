package cn.qijiv.trigger.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenService {
    private static final String ISSUER = "marketing-platform";
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long expiresInSeconds;

    public JwtTokenService(@Value("${app.config.jwt-secret}") String secret,
                           @Value("${app.config.jwt-expires-seconds:43200}") long expiresInSeconds) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.config.jwt-secret 必须至少 32 个字符");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
        this.expiresInSeconds = expiresInSeconds;
    }

    public String create(String userId, String accountName) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(userId)
                .withClaim("accountName", accountName)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(expiresInSeconds)))
                .sign(algorithm);
    }

    public String verifyAndGetUserId(String token) {
        DecodedJWT jwt = verifier.verify(token);
        return jwt.getSubject();
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }
}
