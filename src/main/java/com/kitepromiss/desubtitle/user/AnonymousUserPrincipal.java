package com.kitepromiss.desubtitle.user;

/**
 * 已通过校验的匿名会话：{@code userId} 对应 JWT {@code sub}，{@code tokenId} 对应 JWT {@code jti}（与 {@code user_tokens} 主键一致）。
 */
public record AnonymousUserPrincipal(String userId, String tokenId) {

    public AnonymousUserPrincipal {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId");
        }
        if (tokenId == null || tokenId.isEmpty()) {
            throw new IllegalArgumentException("tokenId");
        }
    }
}
