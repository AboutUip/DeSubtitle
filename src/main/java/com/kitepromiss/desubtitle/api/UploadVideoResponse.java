package com.kitepromiss.desubtitle.api;

/**
 * {@code POST /uploadVideo} 成功时的 JSON 体。
 */
public record UploadVideoResponse(
        String id,
        String storedFileName,
        String originalFileName,
        long sizeBytes,
        String contentType) {}
