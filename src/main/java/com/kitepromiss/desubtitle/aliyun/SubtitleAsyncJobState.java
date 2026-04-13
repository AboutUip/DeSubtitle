package com.kitepromiss.desubtitle.aliyun;

/**
 * GetAsyncJobResult 解析后的任务视图（Result 仍为 JSON 字符串，由业务层再取 VideoUrl）。
 */
public record SubtitleAsyncJobState(String status, String resultJson, String errorCode, String errorMessage) {}
