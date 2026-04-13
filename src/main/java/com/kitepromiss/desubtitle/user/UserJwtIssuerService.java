package com.kitepromiss.desubtitle.user;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.kitepromiss.desubtitle.sqlite.SqliteConcurrencyController;

import io.jsonwebtoken.Jwts;

/**
 * 在 {@link SqliteConcurrencyController} 临界区内写入 {@code user_tokens} 并签发带 {@code sub}、{@code jti} 的 HS256 JWT。
 */
@Service
public class UserJwtIssuerService {

    /** HS256 要求至少 256 bit 密钥；按 UTF-8 字节计。 */
    public static final int MIN_SECRET_UTF8_BYTES = 32;

    private final UserTokenLuaSettings luaSettings;
    private final JwtSigningSecretSource signingSecretSource;
    private final SqliteConcurrencyController sqliteConcurrencyController;
    private final UserTokenRepository userTokenRepository;

    public UserJwtIssuerService(
            UserTokenLuaSettings luaSettings,
            JwtSigningSecretSource signingSecretSource,
            SqliteConcurrencyController sqliteConcurrencyController,
            UserTokenRepository userTokenRepository) {
        this.luaSettings = luaSettings;
        this.signingSecretSource = signingSecretSource;
        this.sqliteConcurrencyController = sqliteConcurrencyController;
        this.userTokenRepository = userTokenRepository;
    }

    public record IssuedAnonymousToken(String token, long expiresInSeconds, String anonymousUserId) {}

    public IssuedAnonymousToken issueToken() {
        SecretKey key = UserJwtSignatureSupport.hmacSha256Key(signingSecretSource);

        int ttlMinutes = luaSettings.tokenTtlMinutes();
        long ttlSeconds = ttlMinutes * 60L;
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);
        String sub = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        return sqliteConcurrencyController.supply(() -> {
            userTokenRepository.save(new UserTokenEntity(jti, sub, exp, false, now));
            String compact = Jwts.builder()
                    .id(jti)
                    .subject(sub)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(exp))
                    .signWith(key)
                    .compact();
            return new IssuedAnonymousToken(compact, ttlSeconds, sub);
        });
    }
}
