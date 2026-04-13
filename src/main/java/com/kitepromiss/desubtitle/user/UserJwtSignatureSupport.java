package com.kitepromiss.desubtitle.user;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import io.jsonwebtoken.security.Keys;

/**
 * 自 {@link JwtSigningSecretSource} 构造 HS256 密钥；签发与校验共用同一规则。
 */
public final class UserJwtSignatureSupport {

    private UserJwtSignatureSupport() {}

    public static SecretKey hmacSha256Key(JwtSigningSecretSource source) {
        String raw = source.rawSecret();
        if (raw == null || raw.isEmpty()) {
            throw new JwtSecretNotConfiguredException("JWT 签名密钥不可用（未配置且未生成）");
        }
        byte[] keyBytes = raw.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < UserJwtIssuerService.MIN_SECRET_UTF8_BYTES) {
            throw new JwtSecretNotConfiguredException(
                    "JWT 密钥过短（UTF-8 至少 " + UserJwtIssuerService.MIN_SECRET_UTF8_BYTES + " 字节），请检查环境变量 "
                            + EnvJwtSigningSecretSource.ENV_NAME);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
