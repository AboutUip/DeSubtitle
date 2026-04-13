package com.kitepromiss.desubtitle.user;

import java.security.SecureRandom;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * JWT 签名密钥：优先环境变量 {@value #ENV_NAME}；未设置时于进程启动时生成 32 字节随机密钥，仅驻内存（不写盘、不打印明文）。
 */
@Component
public class EnvJwtSigningSecretSource implements JwtSigningSecretSource {

    private static final Logger log = LoggerFactory.getLogger(EnvJwtSigningSecretSource.class);

    public static final String ENV_NAME = "DESUBTITLE_JWT_SECRET";

    /** 随机源密钥的字节长度（HS256 至少 256 bit）。 */
    private static final int EPHEMERAL_KEY_BYTES = 32;

    private volatile String ephemeralSecret;

    @PostConstruct
    void warmEphemeralIfNeeded() {
        if (readEnv() != null) {
            return;
        }
        ensureEphemeralGenerated();
        log.info(
                "未检测到环境变量 {}，已为本 JVM 生成仅内存中的随机 JWT 密钥（{} 字节，Base64 表示）；进程重启后旧令牌全部失效；多实例须设置统一 {}。",
                ENV_NAME,
                EPHEMERAL_KEY_BYTES,
                ENV_NAME);
    }

    private static String readEnv() {
        String v = System.getenv(ENV_NAME);
        return v == null || v.isBlank() ? null : v.trim();
    }

    private void ensureEphemeralGenerated() {
        if (ephemeralSecret != null) {
            return;
        }
        synchronized (this) {
            if (ephemeralSecret == null) {
                byte[] buf = new byte[EPHEMERAL_KEY_BYTES];
                new SecureRandom().nextBytes(buf);
                ephemeralSecret = Base64.getEncoder().encodeToString(buf);
            }
        }
    }

    @Override
    public String rawSecret() {
        String fromEnv = readEnv();
        if (fromEnv != null) {
            return fromEnv;
        }
        ensureEphemeralGenerated();
        return ephemeralSecret;
    }
}
