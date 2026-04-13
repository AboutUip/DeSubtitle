package com.kitepromiss.desubtitle.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.kitepromiss.desubtitle.sqlite.SqliteConcurrencyController;
import com.kitepromiss.desubtitle.user.JwtSecretNotConfiguredException;
import com.kitepromiss.desubtitle.user.UserJwtIssuerService;
import com.kitepromiss.desubtitle.user.UserTokenLuaSettings;
import com.kitepromiss.desubtitle.user.UserTokenRepository;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

class UserTokenControllerTest {

    private static final WorkspacePaths DUMMY_PATHS = new WorkspacePaths(
            Path.of("a.json"),
            Path.of("b.lua"),
            Path.of("data"),
            Path.of("agreement.json"),
            Path.of("user_token.lua"),
            Path.of("video_upload.lua"));

    private static UserTokenLuaSettings dummyLua() {
        return new UserTokenLuaSettings(DUMMY_PATHS);
    }

    private static UserTokenRepository noopRepo() {
        return (UserTokenRepository) java.lang.reflect.Proxy.newProxyInstance(
                UserTokenRepository.class.getClassLoader(),
                new Class<?>[] {UserTokenRepository.class},
                (p, m, a) -> {
                    if ("save".equals(m.getName())) {
                        return a[0];
                    }
                    Class<?> rt = m.getReturnType();
                    return rt.isPrimitive() && rt != boolean.class ? 0 : null;
                });
    }

    @Test
    void returnsTokenJson() {
        UserJwtIssuerService svc = new UserJwtIssuerService(
                dummyLua(), () -> "0123456789abcdef0123456789abcdef", new SqliteConcurrencyController(), noopRepo()) {
            @Override
            public UserJwtIssuerService.IssuedAnonymousToken issueToken() {
                return new UserJwtIssuerService.IssuedAnonymousToken("jwt-body", 3600, "uid-1");
            }
        };
        ResponseEntity<?> r = new UserTokenController(svc).getUserToken();
        assertEquals(HttpStatus.OK, r.getStatusCode());
        UserTokenController.UserTokenResponse body = assertInstanceOf(UserTokenController.UserTokenResponse.class, r.getBody());
        assertEquals("jwt-body", body.token());
        assertEquals(3600L, body.expiresInSeconds());
        assertEquals("uid-1", body.userId());
    }

    @Test
    void secretMissingReturns503() {
        UserJwtIssuerService svc = new UserJwtIssuerService(
                dummyLua(), () -> "0123456789abcdef0123456789abcdef", new SqliteConcurrencyController(), noopRepo()) {
            @Override
            public UserJwtIssuerService.IssuedAnonymousToken issueToken() {
                throw new JwtSecretNotConfiguredException("x");
            }
        };
        ResponseEntity<?> r = new UserTokenController(svc).getUserToken();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, r.getStatusCode());
        UserTokenController.UserTokenErrorBody body =
                assertInstanceOf(UserTokenController.UserTokenErrorBody.class, r.getBody());
        assertEquals("jwt_secret_not_configured", body.error());
    }
}
