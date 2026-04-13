package com.kitepromiss.desubtitle.user;

import java.util.Base64;
import java.util.Optional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 仅从 JWT 第二段 payload 解析 {@code sub}，<strong>不校验签名</strong>，仅用于在线人数等「替换身份」前的去重。
 */
public final class UnsafeJwtSub {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private UnsafeJwtSub() {}

    public static Optional<String> tryParseSub(String compactJwt) {
        if (compactJwt == null || compactJwt.isBlank()) {
            return Optional.empty();
        }
        String[] parts = compactJwt.split("\\.");
        if (parts.length < 2) {
            return Optional.empty();
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode n = JSON.readTree(payload);
            String sub = n.path("sub").asText("");
            if (sub.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(sub);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
