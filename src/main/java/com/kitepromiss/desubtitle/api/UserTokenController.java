package com.kitepromiss.desubtitle.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kitepromiss.desubtitle.user.EnvJwtSigningSecretSource;
import com.kitepromiss.desubtitle.user.JwtSecretNotConfiguredException;
import com.kitepromiss.desubtitle.user.UserJwtIssuerService;

/**
 * 为匿名访客签发临时 JWT（无登录）；密钥优先环境变量 {@code DESUBTITLE_JWT_SECRET}，否则进程内随机生成（仅存内存），有效期来自 {@code config/lua/user_token.lua}。
 */
@RestController
public class UserTokenController {

    private static final Logger log = LoggerFactory.getLogger(UserTokenController.class);

    private final UserJwtIssuerService userJwtIssuerService;

    public UserTokenController(UserJwtIssuerService userJwtIssuerService) {
        this.userJwtIssuerService = userJwtIssuerService;
    }

    /**
     * 无查询参数、无请求体；与 JWT {@code sub} 一致的用户标识便于前端展示或关联本地状态（勿在未验签前信任为安全身份）。
     */
    @GetMapping("/getUserToken")
    public ResponseEntity<?> getUserToken() {
        try {
            UserJwtIssuerService.IssuedAnonymousToken t = userJwtIssuerService.issueToken();
            return ResponseEntity.ok(new UserTokenResponse(t.token(), t.expiresInSeconds(), t.anonymousUserId()));
        } catch (JwtSecretNotConfiguredException e) {
            log.warn(
                    "GET /getUserToken 返回 503：JWT 密钥不可用（例如 {} 已设置但 UTF-8 长度不足 32 字节）。参见 config/lua/user_token.lua。",
                    EnvJwtSigningSecretSource.ENV_NAME);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new UserTokenErrorBody("jwt_secret_not_configured"));
        }
    }

    public record UserTokenResponse(String token, long expiresInSeconds, String userId) {}

    public record UserTokenErrorBody(String error) {}
}
