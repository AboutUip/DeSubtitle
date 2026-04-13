package com.kitepromiss.desubtitle.credential;

/**
 * 调用阿里云 OpenAPI 时使用的 AccessKey 对（来自 SQLite、调试内存或环境变量）。
 */
public record ResolvedAliyunKeys(String accessKeyId, String accessKeySecret) {}
