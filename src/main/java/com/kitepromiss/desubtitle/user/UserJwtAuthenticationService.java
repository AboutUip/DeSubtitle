package com.kitepromiss.desubtitle.user;

import java.time.Instant;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.kitepromiss.desubtitle.sqlite.SqliteConcurrencyController;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

/**
 * 验签 JWT，并在 {@link SqliteConcurrencyController} 临界区内核对 {@code user_tokens} 行是否未撤销且未过期；成功时返回
 * {@link AnonymousUserPrincipal}。
 */
@Service
public class UserJwtAuthenticationService implements AnonymousUserJwtGate {

    private final JwtSigningSecretSource signingSecretSource;
    private final SqliteConcurrencyController sqliteConcurrencyController;
    private final UserTokenRepository userTokenRepository;

    public UserJwtAuthenticationService(
            JwtSigningSecretSource signingSecretSource,
            SqliteConcurrencyController sqliteConcurrencyController,
            UserTokenRepository userTokenRepository) {
        this.signingSecretSource = signingSecretSource;
        this.sqliteConcurrencyController = sqliteConcurrencyController;
        this.userTokenRepository = userTokenRepository;
    }

    @Override
    public Optional<AnonymousUserPrincipal> verifyCompactJwt(String compactJwt) {
        if (compactJwt == null || compactJwt.isBlank()) {
            return Optional.empty();
        }
        SecretKey key;
        try {
            key = UserJwtSignatureSupport.hmacSha256Key(signingSecretSource);
        } catch (JwtSecretNotConfiguredException e) {
            return Optional.empty();
        }
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(compactJwt.trim()).getPayload();
        } catch (JwtException e) {
            return Optional.empty();
        }
        String jti = claims.getId();
        String sub = claims.getSubject();
        if (jti == null || jti.isEmpty() || sub == null || sub.isEmpty()) {
            return Optional.empty();
        }
        return sqliteConcurrencyController.supply(() -> userTokenRepository
                .findById(jti)
                .filter(e -> sub.equals(e.getUserId()))
                .filter(e -> !e.isRevoked())
                .filter(e -> !e.getExpiresAt().isBefore(Instant.now()))
                .map(e -> new AnonymousUserPrincipal(sub, jti)));
    }
}
