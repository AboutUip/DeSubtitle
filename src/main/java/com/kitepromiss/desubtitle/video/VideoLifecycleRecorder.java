package com.kitepromiss.desubtitle.video;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kitepromiss.desubtitle.indicator.VideoLifecycleDetail;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

/**
 * 记录已上传视频的过期时刻，周期性删除 {@code data/videos/} 下过期的文件及对应库行；并为指标快照提供每视频剩余秒数。
 */
@Component
public class VideoLifecycleRecorder {

    private static final Logger log = LoggerFactory.getLogger(VideoLifecycleRecorder.class);

    private static final int ERROR_SNIP = 512;

    private final UserVideoRepository userVideoRepository;
    private final WorkspacePaths workspacePaths;

    public VideoLifecycleRecorder(UserVideoRepository userVideoRepository, WorkspacePaths workspacePaths) {
        this.userVideoRepository = userVideoRepository;
        this.workspacePaths = workspacePaths;
    }

    /**
     * @param nowEpochMillis 与指标快照 {@code capturedAtEpochMillis} 对齐，便于与同一时刻的 counters/gauges 对照。
     * @return 视频 id → 剩余整秒数（已过期尚未被清理调度跑完前为 0）。
     */
    public Map<String, Long> secondsUntilExpiryAtMillis(long nowEpochMs) {
        TreeMap<String, Long> out = new TreeMap<>();
        for (UserVideoEntity e : userVideoRepository.findAll()) {
            long expMs = e.getExpiresAt().toEpochMilli();
            long sec = Math.max(0L, (expMs - nowEpochMs) / 1000L);
            out.put(e.getId(), sec);
        }
        return Map.copyOf(out);
    }

    /**
     * 全库视频在指定时刻的生命周期明细（{@code videoId} 字典序），供 {@code GET /life} 的 {@code indicators} 等使用。
     */
    public List<VideoLifecycleDetail> videoLifecyclesAtMillis(long nowEpochMs) {
        List<UserVideoEntity> all = new ArrayList<>(userVideoRepository.findAll());
        all.sort(Comparator.comparing(UserVideoEntity::getId));
        List<VideoLifecycleDetail> out = new ArrayList<>(all.size());
        for (UserVideoEntity e : all) {
            out.add(toLifecycleDetail(e, nowEpochMs));
        }
        return List.copyOf(out);
    }

    /**
     * 指定用户在快照时刻的视频生命周期（按 {@code created_at} 升序）。
     */
    public List<VideoLifecycleDetail> videoLifecyclesForUserAtMillis(String userId, long nowEpochMs) {
        return userVideoRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(e -> toLifecycleDetail(e, nowEpochMs))
                .toList();
    }

    static VideoLifecycleDetail toLifecycleDetail(UserVideoEntity e, long nowEpochMs) {
        long uploadExpMs = e.getExpiresAt().toEpochMilli();
        long uploadSec = Math.max(0L, (uploadExpMs - nowEpochMs) / 1000L);
        Long outExpMs = e.getDesubtitleOutputExpiresAt() != null
                ? e.getDesubtitleOutputExpiresAt().toEpochMilli()
                : null;
        Long outSec = outExpMs == null ? null : Math.max(0L, (outExpMs - nowEpochMs) / 1000L);
        String err = e.getDesubtitleError();
        if (err != null && err.length() > ERROR_SNIP) {
            err = err.substring(0, ERROR_SNIP);
        }
        return new VideoLifecycleDetail(
                e.getId(),
                e.getUserId(),
                e.getStoredFileName(),
                e.getOriginalFileName(),
                e.getContentType(),
                e.getSizeBytes(),
                e.getCreatedAt().toEpochMilli(),
                uploadExpMs,
                uploadSec,
                e.getDesubtitleJobId(),
                e.getDesubtitleLastStatus(),
                err,
                e.getDesubtitleOutputFileName(),
                outExpMs,
                outSec);
    }

    @Scheduled(fixedDelayString = "${desubtitle.video.lifecycle-purge-interval-ms:15000}")
    public void purgeExpired() {
        purgeExpiredNow();
    }

    @Transactional
    public void purgeExpiredNow() {
        Instant now = Instant.now();
        purgeExpiredDesubtitleOutputs(now);
        List<UserVideoEntity> expired = userVideoRepository.findByExpiresAtBefore(now);
        Path dir = workspacePaths.dataDirectory().resolve("videos");
        Path desubDir = workspacePaths.dataDirectory().resolve("desubtitle");
        for (UserVideoEntity e : expired) {
            VideoStorageFileNames.safeResolve(dir, e.getStoredFileName())
                    .ifPresentOrElse(
                            file -> {
                                try {
                                    Files.deleteIfExists(file);
                                } catch (Exception ex) {
                                    log.warn("删除过期视频文件失败 {}: {}", file, ex.toString());
                                }
                            },
                            () -> log.warn("跳过删除非法源文件名: videoId={}", e.getId()));
            deleteDesubtitleOutputFileIfPresent(desubDir, e.getDesubtitleOutputFileName());
            userVideoRepository.delete(e);
        }
    }

    /**
     * 去字幕产物单独到期：删 {@code data/desubtitle/} 下文件并清空输出相关列（源上传仍在保留期内时行不删）。
     */
    private void purgeExpiredDesubtitleOutputs(Instant now) {
        List<UserVideoEntity> rows = userVideoRepository.findOutputExpiredBefore(now);
        Path desubDir = workspacePaths.dataDirectory().resolve("desubtitle");
        for (UserVideoEntity e : rows) {
            deleteDesubtitleOutputFileIfPresent(desubDir, e.getDesubtitleOutputFileName());
            e.setDesubtitleOutputFileName(null);
            e.setDesubtitleOutputExpiresAt(null);
            userVideoRepository.save(e);
        }
    }

    private void deleteDesubtitleOutputFileIfPresent(Path desubDir, String outputName) {
        if (outputName == null || outputName.isBlank()) {
            return;
        }
        Optional<Path> opt = VideoStorageFileNames.safeResolve(desubDir, outputName);
        if (opt.isEmpty()) {
            log.warn("跳过删除非法去字幕成品名");
            return;
        }
        Path f = opt.get();
        try {
            Files.deleteIfExists(f);
        } catch (Exception ex) {
            log.warn("删除去字幕产物失败 {}: {}", f, ex.toString());
        }
    }
}
