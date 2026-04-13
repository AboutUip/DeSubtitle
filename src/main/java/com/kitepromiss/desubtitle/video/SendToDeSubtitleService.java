package com.kitepromiss.desubtitle.video;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.kitepromiss.desubtitle.aliyun.SubtitleAsyncJobState;
import com.kitepromiss.desubtitle.aliyun.SubtitleEraseBand;
import com.kitepromiss.desubtitle.indicator.IndicatorRecorder;
import com.kitepromiss.desubtitle.aliyun.VideoenhanSubtitleEraseOperations;
import com.kitepromiss.desubtitle.api.SubtitleVerticalPosition;
import com.kitepromiss.desubtitle.api.SendToDeSubtitleBatchResponse;
import com.kitepromiss.desubtitle.api.SendToDeSubtitleItemResult;
import com.kitepromiss.desubtitle.credential.AliyunCredentialsSource;
import com.kitepromiss.desubtitle.credential.MissingAliyunCredentialsException;
import com.kitepromiss.desubtitle.credential.ResolvedAliyunKeys;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 将当前用户本地上传视频提交阿里云 EraseVideoSubtitles（Advance），轮询结果并下载成品至 {@code data/desubtitle/}。
 *
 * <p>同一 {@code userId} 下批量与单条提交在 {@link UserIdStripeLock} 上串行，并与 {@link UserVideoRevocationService} 互斥，避免撤销与去字幕交错。
 */
@Service
public class SendToDeSubtitleService {

    /** 每用户调用 {@code POST /sendToDeSubtitle} 次数计数器前缀。 */
    public static final String METRIC_DESUB_BATCH_REQUESTS_USER_PREFIX = "desubtitle_batch_requests_user_";

    /** 每用户调用 {@code POST /sendVideoToDeSubtitle} 次数计数器前缀。 */
    public static final String METRIC_DESUB_SINGLE_REQUESTS_USER_PREFIX = "desubtitle_single_requests_user_";

    /** 每用户成功完成一次去字幕落盘（单条结果 {@code success}）计数器前缀。 */
    public static final String METRIC_DESUB_JOB_SUCCESS_USER_PREFIX = "desubtitle_job_success_user_";

    static final String STATUS_PROCESS_SUCCESS = "PROCESS_SUCCESS";
    static final String STATUS_PROCESS_FAILED = "PROCESS_FAILED";
    static final String STATUS_TIMEOUT_FAILED = "TIMEOUT_FAILED";
    static final String STATUS_LIMIT_RETRY_FAILED = "LIMIT_RETRY_FAILED";
    static final String STATUS_QUEUING = "QUEUING";
    static final String STATUS_PROCESSING = "PROCESSING";

    static final String LOCAL_SUBMIT_FAILED = "SUBMIT_FAILED";
    static final String LOCAL_POLL_TIMEOUT = "LOCAL_POLL_TIMEOUT";
    static final String LOCAL_DOWNLOAD_FAILED = "DOWNLOAD_FAILED";
    static final String LOCAL_MISSING_VIDEO_URL = "MISSING_VIDEO_URL";

    static final String OUTCOME_SUCCESS = "success";
    static final String OUTCOME_SKIPPED = "skipped";
    static final String OUTCOME_FAILED = "failed";
    static final String OUTCOME_TIMEOUT = "timeout";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final int ERROR_MAX = 2000;

    private final WorkspacePaths workspacePaths;
    private final UserVideoRepository userVideoRepository;
    private final VideoUploadLuaSettings videoUploadLuaSettings;
    private final IndicatorRecorder indicatorRecorder;
    private final UserIdStripeLock userIdStripeLock;
    private final AliyunCredentialsSource aliyunCredentialsSource;
    private final VideoenhanSubtitleEraseOperations videoenhanSubtitleEraseOperations;

    public SendToDeSubtitleService(
            WorkspacePaths workspacePaths,
            UserVideoRepository userVideoRepository,
            VideoUploadLuaSettings videoUploadLuaSettings,
            IndicatorRecorder indicatorRecorder,
            UserIdStripeLock userIdStripeLock,
            AliyunCredentialsSource aliyunCredentialsSource,
            VideoenhanSubtitleEraseOperations videoenhanSubtitleEraseOperations) {
        this.workspacePaths = workspacePaths;
        this.userVideoRepository = userVideoRepository;
        this.videoUploadLuaSettings = videoUploadLuaSettings;
        this.indicatorRecorder = indicatorRecorder;
        this.userIdStripeLock = userIdStripeLock;
        this.aliyunCredentialsSource = aliyunCredentialsSource;
        this.videoenhanSubtitleEraseOperations = videoenhanSubtitleEraseOperations;
    }

    public SendToDeSubtitleBatchResponse sendAllForUser(String userId) {
        ResolvedAliyunKeys keys =
                aliyunCredentialsSource.resolve().orElseThrow(MissingAliyunCredentialsException::new);
        synchronized (userIdStripeLock.lockFor(userId)) {
            indicatorRecorder.incrementCounter(
                    METRIC_DESUB_BATCH_REQUESTS_USER_PREFIX + VideoUploadService.metricSuffixForUser(userId));

            Instant now = Instant.now();
            Path videoDir = workspacePaths.dataDirectory().resolve("videos");
            Path outDir = workspacePaths.dataDirectory().resolve("desubtitle");
            try {
                Files.createDirectories(outDir);
            } catch (IOException e) {
                throw new IllegalStateException("无法创建 desubtitle 目录: " + e.getMessage(), e);
            }

            List<UserVideoEntity> rows = userVideoRepository.findByUserIdOrderByCreatedAtAsc(userId);
            List<SendToDeSubtitleItemResult> results = new ArrayList<>();
            for (UserVideoEntity e : rows) {
                results.add(processOne(e, userId, keys, videoDir, outDir, now, null, true));
            }
            return new SendToDeSubtitleBatchResponse(List.copyOf(results));
        }
    }

    /**
     * 仅处理指定 id 的视频（须属于当前用户）；不存在或不属于该用户时抛出 {@link UserVideoNotFoundException}。
     *
     * <p>字幕竖直位置缺省为 {@link SubtitleVerticalPosition#BOTTOM}。
     */
    public SendToDeSubtitleItemResult sendSingleForUser(String userId, String videoId) {
        return sendSingleForUser(userId, videoId, SubtitleVerticalPosition.BOTTOM);
    }

    /**
     * 单条去字幕；{@code subtitlePosition} 为 {@code null} 时按 {@link SubtitleVerticalPosition#BOTTOM} 处理。
     */
    public SendToDeSubtitleItemResult sendSingleForUser(
            String userId, String videoId, SubtitleVerticalPosition subtitlePosition) {
        ResolvedAliyunKeys keys =
                aliyunCredentialsSource.resolve().orElseThrow(MissingAliyunCredentialsException::new);
        synchronized (userIdStripeLock.lockFor(userId)) {
            indicatorRecorder.incrementCounter(
                    METRIC_DESUB_SINGLE_REQUESTS_USER_PREFIX + VideoUploadService.metricSuffixForUser(userId));

            UserVideoEntity e = userVideoRepository
                    .findById(videoId)
                    .filter(v -> userId.equals(v.getUserId()))
                    .orElseThrow(() -> new UserVideoNotFoundException(videoId));

            Instant now = Instant.now();
            Path videoDir = workspacePaths.dataDirectory().resolve("videos");
            Path outDir = workspacePaths.dataDirectory().resolve("desubtitle");
            try {
                Files.createDirectories(outDir);
            } catch (IOException ex) {
                throw new IllegalStateException("无法创建 desubtitle 目录: " + ex.getMessage(), ex);
            }
            SubtitleVerticalPosition pos =
                    subtitlePosition != null ? subtitlePosition : SubtitleVerticalPosition.BOTTOM;
            return processOne(e, userId, keys, videoDir, outDir, now, toEraseBand(pos), false);
        }
    }

    private static SubtitleEraseBand toEraseBand(SubtitleVerticalPosition p) {
        if (p == SubtitleVerticalPosition.FULL) {
            return new SubtitleEraseBand(0f, 0f, 1f, 1f);
        }
        return new SubtitleEraseBand(0f, p.by(), 1f, p.bh());
    }

    private SendToDeSubtitleItemResult processOne(
            UserVideoEntity e,
            String userId,
            ResolvedAliyunKeys keys,
            Path videoDir,
            Path outDir,
            Instant now,
            SubtitleEraseBand regionOverride,
            boolean skipIfAlreadyProcessed) {
        UserVideoEntity current = userVideoRepository.findById(e.getId()).orElse(null);
        if (current == null) {
            return new SendToDeSubtitleItemResult(
                    e.getId(), OUTCOME_SKIPPED, null, null, null, "record_removed");
        }
        if (!userId.equals(current.getUserId())) {
            return new SendToDeSubtitleItemResult(
                    e.getId(), OUTCOME_FAILED, null, null, null, "user_mismatch");
        }
        e = current;

        if (!e.getExpiresAt().isAfter(now)) {
            return new SendToDeSubtitleItemResult(
                    e.getId(), OUTCOME_SKIPPED, e.getDesubtitleLastStatus(), null, null, "upload_expired");
        }
        Optional<Path> sourceOpt = VideoStorageFileNames.safeResolve(videoDir, e.getStoredFileName());
        if (sourceOpt.isEmpty()) {
            e.setDesubtitleLastStatus(LOCAL_SUBMIT_FAILED);
            e.setDesubtitleError(trunc("非法或不可解析的源文件名"));
            userVideoRepository.save(e);
            return new SendToDeSubtitleItemResult(
                    e.getId(), OUTCOME_FAILED, e.getDesubtitleLastStatus(), null, null, e.getDesubtitleError());
        }
        Path source = sourceOpt.get();
        if (!Files.isRegularFile(source)) {
            e.setDesubtitleLastStatus(LOCAL_SUBMIT_FAILED);
            e.setDesubtitleError(trunc("源文件不存在: " + source));
            userVideoRepository.save(e);
            return new SendToDeSubtitleItemResult(
                    e.getId(), OUTCOME_FAILED, e.getDesubtitleLastStatus(), null, null, e.getDesubtitleError());
        }
        if (skipIfAlreadyProcessed && shouldSkipAlreadyDone(e, outDir)) {
            Long expMs = e.getDesubtitleOutputExpiresAt() != null
                    ? e.getDesubtitleOutputExpiresAt().toEpochMilli()
                    : null;
            return new SendToDeSubtitleItemResult(
                    e.getId(),
                    OUTCOME_SKIPPED,
                    e.getDesubtitleLastStatus(),
                    e.getDesubtitleOutputFileName(),
                    expMs,
                    "already_processed");
        }
        if (!skipIfAlreadyProcessed) {
            prepareRedoDesubtitle(e, outDir);
            userVideoRepository.save(e);
            e = userVideoRepository.findById(e.getId()).orElse(e);
        }

        int pollTimeoutMin = videoUploadLuaSettings.desubtitlePollTimeoutMinutes();
        Instant deadline = Instant.now().plus(Duration.ofMinutes(pollTimeoutMin));
        int maxSubmits = 2;
        int submits = 0;

        try {
            while (true) {
                boolean needSubmit =
                        e.getDesubtitleJobId() == null
                                || e.getDesubtitleJobId().isBlank()
                                || STATUS_LIMIT_RETRY_FAILED.equals(e.getDesubtitleLastStatus());
                if (needSubmit) {
                    if (submits >= maxSubmits) {
                        e.setDesubtitleError(trunc("超过提交重试次数"));
                        userVideoRepository.save(e);
                        return itemFailed(e, "submit_retry_exhausted");
                    }
                    submits++;
                    e.setDesubtitleJobId(null);
                    e.setDesubtitleLastStatus(null);
                    e.setDesubtitleError(null);
                    String jobId =
                            videoenhanSubtitleEraseOperations.submitEraseLocalFile(
                                    source,
                                    keys.accessKeyId(),
                                    keys.accessKeySecret(),
                                    regionOverride);
                    e.setDesubtitleJobId(jobId);
                    e.setDesubtitleLastStatus(STATUS_QUEUING);
                    userVideoRepository.save(e);
                }

                PollOutcome po = pollUntilTerminal(e, keys, deadline);
                if (po.limitRetryBranch()) {
                    e.setDesubtitleLastStatus(STATUS_LIMIT_RETRY_FAILED);
                    e.setDesubtitleJobId(null);
                    e.setDesubtitleError(trunc(po.message()));
                    userVideoRepository.save(e);
                    continue;
                }
                if (po.timedOut()) {
                    e.setDesubtitleLastStatus(LOCAL_POLL_TIMEOUT);
                    e.setDesubtitleError(trunc(po.message()));
                    userVideoRepository.save(e);
                    Long expMs = e.getDesubtitleOutputExpiresAt() != null
                            ? e.getDesubtitleOutputExpiresAt().toEpochMilli()
                            : null;
                    return new SendToDeSubtitleItemResult(
                            e.getId(),
                            OUTCOME_TIMEOUT,
                            e.getDesubtitleLastStatus(),
                            e.getDesubtitleOutputFileName(),
                            expMs,
                            po.message());
                }
                if (po.successful()) {
                    return finishDownload(e, outDir, po.resultJson());
                }
                e.setDesubtitleLastStatus(po.terminalStatus());
                e.setDesubtitleError(trunc(po.message()));
                userVideoRepository.save(e);
                return new SendToDeSubtitleItemResult(
                        e.getId(),
                        OUTCOME_FAILED,
                        e.getDesubtitleLastStatus(),
                        null,
                        null,
                        po.message());
            }
        } catch (Exception ex) {
            e.setDesubtitleLastStatus(LOCAL_SUBMIT_FAILED);
            e.setDesubtitleError(trunc(ex.getMessage()));
            userVideoRepository.save(e);
            return new SendToDeSubtitleItemResult(
                    e.getId(), OUTCOME_FAILED, e.getDesubtitleLastStatus(), null, null, ex.getMessage());
        }
    }

    private SendToDeSubtitleItemResult itemFailed(UserVideoEntity e, String err) {
        return new SendToDeSubtitleItemResult(
                e.getId(), OUTCOME_FAILED, e.getDesubtitleLastStatus(), null, null, err);
    }

    /**
     * 单条去字幕时不再调用本逻辑（{@code skipIfAlreadyProcessed=false}），以便用户更换字幕区域后同一视频可重新跑阿里云任务。
     */
    private void prepareRedoDesubtitle(UserVideoEntity e, Path outDir) {
        String outName = e.getDesubtitleOutputFileName();
        if (outName != null && !outName.isBlank()) {
            Optional<Path> pOpt = VideoStorageFileNames.safeResolve(outDir, outName);
            if (pOpt.isPresent()) {
                try {
                    Files.deleteIfExists(pOpt.get());
                } catch (IOException ignored) {
                    // 尽力删除旧成品，失败时仍以新文件名落盘
                }
            }
        }
        e.setDesubtitleOutputFileName(null);
        e.setDesubtitleOutputExpiresAt(null);
        e.setDesubtitleJobId(null);
        e.setDesubtitleLastStatus(null);
        e.setDesubtitleError(null);
    }

    private boolean shouldSkipAlreadyDone(UserVideoEntity e, Path outDir) {
        if (!STATUS_PROCESS_SUCCESS.equals(e.getDesubtitleLastStatus())) {
            return false;
        }
        String name = e.getDesubtitleOutputFileName();
        if (name == null || name.isBlank()) {
            return false;
        }
        Optional<Path> pOpt = VideoStorageFileNames.safeResolve(outDir, name);
        if (pOpt.isEmpty() || !Files.isRegularFile(pOpt.get())) {
            return false;
        }
        Instant exp = e.getDesubtitleOutputExpiresAt();
        return exp == null || exp.isAfter(Instant.now());
    }

    private PollOutcome pollUntilTerminal(UserVideoEntity e, ResolvedAliyunKeys keys, Instant deadline)
            throws Exception {
        Duration backoff = Duration.ofSeconds(2);
        String jobId = e.getDesubtitleJobId();
        while (Instant.now().isBefore(deadline)) {
            SubtitleAsyncJobState st =
                    videoenhanSubtitleEraseOperations.getAsyncJob(
                            jobId, keys.accessKeyId(), keys.accessKeySecret());
            String status = st.status();
            e.setDesubtitleLastStatus(status != null ? status : e.getDesubtitleLastStatus());
            if (status == null || status.isBlank()) {
                e.setDesubtitleError(trunc(joinErr(st.errorCode(), st.errorMessage())));
                userVideoRepository.save(e);
                Thread.sleep(backoffMillis(backoff));
                backoff = bumpBackoff(backoff);
                continue;
            }
            if (STATUS_PROCESS_SUCCESS.equals(status)) {
                userVideoRepository.save(e);
                return PollOutcome.success(st.resultJson());
            }
            if (STATUS_LIMIT_RETRY_FAILED.equals(status)) {
                return PollOutcome.limitRetry();
            }
            if (STATUS_PROCESS_FAILED.equals(status)
                    || STATUS_TIMEOUT_FAILED.equals(status)) {
                String msg = joinErr(st.errorCode(), st.errorMessage());
                return PollOutcome.failedTerminal(status, msg);
            }
            if (STATUS_QUEUING.equals(status) || STATUS_PROCESSING.equals(status)) {
                userVideoRepository.save(e);
                Thread.sleep(backoffMillis(backoff));
                backoff = bumpBackoff(backoff);
                continue;
            }
            e.setDesubtitleError(trunc(joinErr(st.errorCode(), st.errorMessage())));
            userVideoRepository.save(e);
            Thread.sleep(backoffMillis(backoff));
            backoff = bumpBackoff(backoff);
        }
        return PollOutcome.timeout("轮询超时（desubtitle_poll_timeout_minutes）");
    }

    private SendToDeSubtitleItemResult finishDownload(UserVideoEntity e, Path outDir, String resultJson) {
        try {
            String url = extractVideoUrl(resultJson);
            if (url == null || url.isBlank()) {
                e.setDesubtitleLastStatus(LOCAL_MISSING_VIDEO_URL);
                e.setDesubtitleError(trunc("Result 中无 VideoUrl"));
                userVideoRepository.save(e);
                return new SendToDeSubtitleItemResult(
                        e.getId(), OUTCOME_FAILED, e.getDesubtitleLastStatus(), null, null, "missing_video_url");
            }
            String ext = extensionFromUrl(url);
            String outName = UUID.randomUUID() + ext;
            Path target = outDir.resolve(outName);
            downloadToFile(url, target);
            int retainMin = videoUploadLuaSettings.desubtitleOutputRetentionMinutes();
            Instant outExp = Instant.now().plus(Duration.ofMinutes(retainMin));
            e.setDesubtitleOutputFileName(outName);
            e.setDesubtitleOutputExpiresAt(outExp);
            e.setDesubtitleError(null);
            userVideoRepository.save(e);
            indicatorRecorder.incrementCounter(
                    METRIC_DESUB_JOB_SUCCESS_USER_PREFIX + VideoUploadService.metricSuffixForUser(e.getUserId()));
            return new SendToDeSubtitleItemResult(
                    e.getId(),
                    OUTCOME_SUCCESS,
                    STATUS_PROCESS_SUCCESS,
                    outName,
                    outExp.toEpochMilli(),
                    null);
        } catch (Exception ex) {
            e.setDesubtitleLastStatus(LOCAL_DOWNLOAD_FAILED);
            e.setDesubtitleError(trunc(ex.getMessage()));
            userVideoRepository.save(e);
            return new SendToDeSubtitleItemResult(
                    e.getId(), OUTCOME_FAILED, e.getDesubtitleLastStatus(), null, null, ex.getMessage());
        }
    }

    private static String extractVideoUrl(String resultJson) throws IOException {
        if (resultJson == null || resultJson.isBlank()) {
            return null;
        }
        JsonNode n = JSON.readTree(resultJson);
        JsonNode v = n.get("VideoUrl");
        if (v == null || v.isNull()) {
            return null;
        }
        return v.asText();
    }

    private static void downloadToFile(String url, Path target) throws IOException, InterruptedException {
        HttpClient client =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(45))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
        HttpRequest req =
                HttpRequest.newBuilder(URI.create(url))
                        .GET()
                        .timeout(Duration.ofMinutes(15))
                        .build();
        HttpResponse<Path> res = client.send(req, HttpResponse.BodyHandlers.ofFile(target));
        if (res.statusCode() / 100 != 2) {
            Files.deleteIfExists(target);
            throw new IOException("下载失败 HTTP " + res.statusCode());
        }
    }

    private static String extensionFromUrl(String url) {
        try {
            String path = URI.create(url).getPath();
            if (path == null) {
                return ".mp4";
            }
            int dot = path.lastIndexOf('.');
            if (dot < 0 || dot >= path.length() - 1) {
                return ".mp4";
            }
            String ext = path.substring(dot).toLowerCase();
            if (ext.length() > 8 || !ext.matches("\\.[a-z0-9]+")) {
                return ".mp4";
            }
            return ext;
        } catch (Exception e) {
            return ".mp4";
        }
    }

    private static long backoffMillis(Duration d) {
        long ms = d.toMillis();
        return Math.min(15_000L, Math.max(1_000L, ms));
    }

    private static Duration bumpBackoff(Duration d) {
        Duration n = d.plusSeconds(2);
        if (n.compareTo(Duration.ofSeconds(15)) > 0) {
            return Duration.ofSeconds(15);
        }
        return n;
    }

    private static String joinErr(String code, String msg) {
        if (code == null || code.isBlank()) {
            return msg != null ? msg : "";
        }
        if (msg == null || msg.isBlank()) {
            return code;
        }
        return code + ": " + msg;
    }

    private static String trunc(String s) {
        if (s == null) {
            return null;
        }
        if (s.length() <= ERROR_MAX) {
            return s;
        }
        return s.substring(0, ERROR_MAX);
    }

    private record PollOutcome(
            boolean successful,
            boolean limitRetryBranch,
            boolean timedOut,
            String resultJson,
            String terminalStatus,
            String message) {

        static PollOutcome success(String resultJson) {
            return new PollOutcome(true, false, false, resultJson, STATUS_PROCESS_SUCCESS, null);
        }

        static PollOutcome limitRetry() {
            return new PollOutcome(false, true, false, null, null, "LIMIT_RETRY_FAILED");
        }

        static PollOutcome timeout(String msg) {
            return new PollOutcome(false, false, true, null, null, msg);
        }

        static PollOutcome failedTerminal(String status, String msg) {
            return new PollOutcome(false, false, false, null, status, msg);
        }
    }
}
