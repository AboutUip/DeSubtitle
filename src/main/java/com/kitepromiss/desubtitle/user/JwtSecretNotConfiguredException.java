package com.kitepromiss.desubtitle.user;

/**
 * 未配置或长度不足的 JWT 签名密钥时抛出，由 {@link com.kitepromiss.desubtitle.api.UserTokenController} 映射为 503。
 */
public class JwtSecretNotConfiguredException extends RuntimeException {

    public JwtSecretNotConfiguredException(String message) {
        super(message);
    }
}
