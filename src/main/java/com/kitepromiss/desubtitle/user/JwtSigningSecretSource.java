package com.kitepromiss.desubtitle.user;

/**
 * JWT HS256 签名密钥来源；优先环境变量 {@value EnvJwtSigningSecretSource#ENV_NAME}，未设置时由 {@link EnvJwtSigningSecretSource} 在进程内生成仅内存密钥。
 */
@FunctionalInterface
public interface JwtSigningSecretSource {

    /**
     * @return 原始密钥字符串；未配置或长度不足时返回 {@code null} 或空串（由签发逻辑判定为不可用）
     */
    String rawSecret();
}
