package com.kitepromiss.desubtitle.user;

import java.util.Optional;

/**
 * 校验紧凑序列化 JWT 是否仍有效（签名、未撤销、未过期）；成功时返回 {@link AnonymousUserPrincipal}。
 */
public interface AnonymousUserJwtGate {

    /**
     * @param compactJwt 无 {@code Bearer} 前缀的 JWT 字符串
     * @return 有效时返回主体，否则 empty
     */
    Optional<AnonymousUserPrincipal> verifyCompactJwt(String compactJwt);
}
