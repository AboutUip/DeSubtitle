package com.kitepromiss.desubtitle.user;

/**
 * 通过 Bearer JWT 校验后，将匿名用户标识与完整主体写入请求属性，供后续控制器与 {@link com.kitepromiss.desubtitle.userdata.UserDataManager} 读取。
 */
public final class AnonymousUserRequestAttributes {

    public static final String ANONYMOUS_USER_ID = "com.kitepromiss.desubtitle.anonymousUserId";

    /** {@link AnonymousUserPrincipal}，含 {@code jti}，供撤销会话等。 */
    public static final String ANONYMOUS_USER_PRINCIPAL = "com.kitepromiss.desubtitle.anonymousUserPrincipal";

    private AnonymousUserRequestAttributes() {}
}
