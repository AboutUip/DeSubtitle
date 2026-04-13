package com.kitepromiss.desubtitle.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import com.kitepromiss.desubtitle.sqlite.SqliteConcurrencyController;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class UserJwtAuthenticationServiceTest {

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
    private static UserTokenRepository mapBackedRepo(Map<String, UserTokenEntity> map) {
        return (UserTokenRepository) Proxy.newProxyInstance(
                UserTokenRepository.class.getClassLoader(),
                new Class<?>[] {UserTokenRepository.class},
                (p, m, a) -> {
                    String n = m.getName();
                    if ("save".equals(n)) {
                        UserTokenEntity e = (UserTokenEntity) a[0];
                        map.put(e.getJti(), e);
                        return e;
                    }
                    if ("findById".equals(n)) {
                        return Optional.ofNullable(map.get(a[0]));
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
    void roundTripWithPersistedRow() {
        Map<String, UserTokenEntity> map = new ConcurrentHashMap<>();
        UserTokenRepository repo = mapBackedRepo(map);
        SqliteConcurrencyController sqlite = new SqliteConcurrencyController();
        UserTokenLuaSettings lua = new UserTokenLuaSettings(dummyPaths()) {
            @Override
            public int tokenTtlMinutes() {
                return 60;
            }
        };
        UserJwtIssuerService issuer = new UserJwtIssuerService(lua, () -> SECRET_32, sqlite, repo);
        UserJwtIssuerService.IssuedAnonymousToken t = issuer.issueToken();

        UserJwtAuthenticationService auth = new UserJwtAuthenticationService(() -> SECRET_32, sqlite, repo);
        AnonymousUserPrincipal p = auth.verifyCompactJwt(t.token()).orElseThrow();
        assertEquals(t.anonymousUserId(), p.userId());
        assertEquals(jtiFromToken(t.token()), p.tokenId());
    }

    @Test
    void revokedTokenRejected() {
        Map<String, UserTokenEntity> map = new ConcurrentHashMap<>();
        UserTokenRepository repo = mapBackedRepo(map);
        SqliteConcurrencyController sqlite = new SqliteConcurrencyController();
        UserTokenLuaSettings lua = new UserTokenLuaSettings(dummyPaths()) {
            @Override
            public int tokenTtlMinutes() {
                return 60;
            }
        };
        UserJwtIssuerService issuer = new UserJwtIssuerService(lua, () -> SECRET_32, sqlite, repo);
        UserJwtIssuerService.IssuedAnonymousToken t = issuer.issueToken();

        SecretKey key = Keys.hmacShaKeyFor(SECRET_32.getBytes(StandardCharsets.UTF_8));
        String jti = Jwts.parser().verifyWith(key).build().parseSignedClaims(t.token()).getPayload().getId();
        new UserTokenManagementService(sqlite, repo).revokeByJti(jti);

        UserJwtAuthenticationService auth = new UserJwtAuthenticationService(() -> SECRET_32, sqlite, repo);
        assertTrue(auth.verifyCompactJwt(t.token()).isEmpty());
    }

    private static String jtiFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_32.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getId();
    }
}
