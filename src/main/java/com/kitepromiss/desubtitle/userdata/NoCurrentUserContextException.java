package com.kitepromiss.desubtitle.userdata;

/**
 * 在非 Web 请求线程、或未经过 Bearer 校验的链路上调用需要「当前用户」的 API 时抛出。
 */
public class NoCurrentUserContextException extends IllegalStateException {

    public NoCurrentUserContextException() {
        super("当前上下文中无匿名用户（缺少有效 Bearer、或不在 DispatcherServlet 请求线程内）");
    }
}
