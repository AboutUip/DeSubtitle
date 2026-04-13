package com.kitepromiss.desubtitle.aliyun;

import java.nio.file.Path;

/**
 * 对阿里云 videoenhan EraseVideoSubtitles（Advance 本地上传）与 GetAsyncJobResult 的封装。
 */
public interface VideoenhanSubtitleEraseOperations {

    /**
     * 以本地文件提交异步擦除，返回 RequestId（即后续查询用的 JobId）。
     *
     * @param regionOverride 非 {@code null} 时以此设置 BX/BY/BW/BH；{@code null} 时由实现按全局配置（如 full/bottom）决定
     */
    String submitEraseLocalFile(
            Path videoFile, String accessKeyId, String accessKeySecret, SubtitleEraseBand regionOverride)
            throws Exception;

    /** 查询异步任务状态与结果 JSON 字符串。 */
    SubtitleAsyncJobState getAsyncJob(String jobId, String accessKeyId, String accessKeySecret) throws Exception;
}
