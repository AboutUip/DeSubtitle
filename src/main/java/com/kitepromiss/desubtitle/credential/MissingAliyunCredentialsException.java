package com.kitepromiss.desubtitle.credential;

/**
 * {@link com.kitepromiss.desubtitle.init.InitService} 在缺少可用 AccessKey 时抛出，由 {@link com.kitepromiss.desubtitle.api.InitController} 映射为 HTTP 428。
 */
public final class MissingAliyunCredentialsException extends RuntimeException {

    public MissingAliyunCredentialsException() {
        super("需要先在 POST /init/credentials 提交 AccessKey");
    }
}
