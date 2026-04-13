package com.kitepromiss.desubtitle.credential;

/**
 * 在 {@link com.kitepromiss.desubtitle.init.InitService} 进入初始化临界区前校验 AccessKey 是否已就绪，并返回与本次 init 一致的 {@code debug_mode} 快照。
 */
public interface CredentialInitPrecondition {

    /**
     * @throws MissingAliyunCredentialsException 未提交或提交无效时
     * @return 当前 {@code debug_mode}，供后续写回 {@code runtime.json} 等逻辑使用
     */
    boolean assertKeysPresentAndGetDebugMode();
}
