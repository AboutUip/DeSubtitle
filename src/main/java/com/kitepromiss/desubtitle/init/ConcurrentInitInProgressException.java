package com.kitepromiss.desubtitle.init;

/**
 * 已有其它请求正在执行 {@link InitService#run()} 的独占段时抛出；由 {@link com.kitepromiss.desubtitle.api.InitController} 映射为 HTTP 409。
 */
public final class ConcurrentInitInProgressException extends RuntimeException {

    public ConcurrentInitInProgressException() {
        super("init already in progress");
    }
}
