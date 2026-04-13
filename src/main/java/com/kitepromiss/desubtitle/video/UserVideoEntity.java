package com.kitepromiss.desubtitle.video;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_videos")
public class UserVideoEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "stored_file_name", nullable = false, length = 256)
    private String storedFileName;

    @Column(name = "original_file_name", length = 512)
    private String originalFileName;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** 阿里云 EraseVideoSubtitles 提交后得到的 RequestId，作 GetAsyncJobResult 的 JobId。 */
    @Column(name = "desubtitle_job_id", length = 128)
    private String desubtitleJobId;

    /** 最近一次异步查询的 Data.Status，或本地上传/下载阶段状态（如 SUBMIT_FAILED）。 */
    @Column(name = "desubtitle_last_status", length = 64)
    private String desubtitleLastStatus;

    @Column(name = "desubtitle_error", length = 2048)
    private String desubtitleError;

    /** 相对于 {@code data/desubtitle/} 的去字幕后视频文件名。 */
    @Column(name = "desubtitle_output_file_name", length = 256)
    private String desubtitleOutputFileName;

    /** 去字幕产物在本地保留到该时刻；到期后由 {@link VideoLifecycleRecorder} 删盘并清空相关列。 */
    @Column(name = "desubtitle_output_expires_at")
    private Instant desubtitleOutputExpiresAt;

    protected UserVideoEntity() {}

    public UserVideoEntity(
            String id,
            String userId,
            String storedFileName,
            String originalFileName,
            String contentType,
            long sizeBytes,
            Instant createdAt,
            Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.storedFileName = storedFileName;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getDesubtitleJobId() {
        return desubtitleJobId;
    }

    public void setDesubtitleJobId(String desubtitleJobId) {
        this.desubtitleJobId = desubtitleJobId;
    }

    public String getDesubtitleLastStatus() {
        return desubtitleLastStatus;
    }

    public void setDesubtitleLastStatus(String desubtitleLastStatus) {
        this.desubtitleLastStatus = desubtitleLastStatus;
    }

    public String getDesubtitleError() {
        return desubtitleError;
    }

    public void setDesubtitleError(String desubtitleError) {
        this.desubtitleError = desubtitleError;
    }

    public String getDesubtitleOutputFileName() {
        return desubtitleOutputFileName;
    }

    public void setDesubtitleOutputFileName(String desubtitleOutputFileName) {
        this.desubtitleOutputFileName = desubtitleOutputFileName;
    }

    public Instant getDesubtitleOutputExpiresAt() {
        return desubtitleOutputExpiresAt;
    }

    public void setDesubtitleOutputExpiresAt(Instant desubtitleOutputExpiresAt) {
        this.desubtitleOutputExpiresAt = desubtitleOutputExpiresAt;
    }
}
