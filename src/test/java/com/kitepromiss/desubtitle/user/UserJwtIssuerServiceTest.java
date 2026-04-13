package com.kitepromiss.desubtitle.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import com.kitepromiss.desubtitle.sqlite.SqliteConcurrencyController;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class UserJwtIssuerServiceTest {

    private static final String SECRET_32 = "0123456789abcdef0123456789abcdef";

    private static WorkspacePaths dummyPaths() {
        return new WorkspacePaths(
                Path.of("r.json"),
                Path.of("m.lua"),
                Path.of("data"),
                Path.of("a.json"),
                Path.of("ut.lua"),
                Path.of("video_upload.lua"));
    }

    @SuppressWarnings("unchecked")
    private static UserTokenRepository noopSaveRepo() {
        return (UserTokenRepository) Proxy.newProxyInstance(
                UserTokenRepository.class.getClassLoader(),
                new Class<?>[] {UserTokenRepository.class},
                (p, m, a) -> {
                    if ("save".equals(m.getName())) {
                        return a[0];
                    }
                    Class<?> rt = m.getReturnType();
                    if (rt == boolean.class) {
                        return false;
                    }
                    if (rt.isPrimitive()) {
                        return 0;
                    }
                    return null;
                });
    }

    @Test
    void issuesVerifiableJwtWithJti() {
        UserTokenLuaSettings lua = new UserTokenLuaSettings(dummyPaths()) {
            @Override
            public int tokenTtlMinutes() {
                return 5;
            }
        };
        UserJwtIssuerService svc = new UserJwtIssuerService(
                lua, () -> SECRET_32, new SqliteConcurrencyController(), noopSaveRepo());
        UserJwtIssuerService.IssuedAnonymousToken t = svc.issueToken();
        assertEquals(300L, t.expiresInSeconds());

        SecretKey key = Keys.hmacShaKeyFor(SECRET_32.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(t.token())
                .getPayload();
        assertEquals(t.anonymousUserId(), claims.getSubject());
        org.junit.jupiter.api.Assertions.assertNotNull(claims.getId());
        long expSec = claims.getExpiration().getTime() / 1000;
        long iatSec = claims.getIssuedAt().getTime() / 1000;
        assertEquals(300L, expSec - iatSec, 2L);
    }

    @Test
    void rejectsShortSecret() {
        UserTokenLuaSettings lua = new UserTokenLuaSettings(dummyPaths()) {
            @Override
            public int tokenTtlMinutes() {
                return 1;
            }
        };
        UserJwtIssuerService svc = new UserJwtIssuerService(
                lua, () -> "tooshort", new SqliteConcurrencyController(), noopSaveRepo());
        assertThrows(JwtSecretNotConfiguredException.class, svc::issueToken);
    }
}
