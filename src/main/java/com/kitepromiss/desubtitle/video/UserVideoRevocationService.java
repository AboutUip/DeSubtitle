package com.kitepromiss.desubtitle.video;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kitepromiss.desubtitle.indicator.IndicatorRecorder;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

/**
 * 当前用户主动撤销上传：删源文件、删去字幕成品（若有）、删库行，并记录指标。
 */
@Service
public class UserVideoRevocationService {

    /** 每用户成功撤销次数计数器名前缀，后接 {@link VideoUploadService#metricSuffixForUser(String)}。 */
    public static final String METRIC_REVOKES_USER_PREFIX = "video_revokes_user_";

    private final WorkspacePaths workspacePaths;
    private final UserVideoRepository userVideoRepository;
    private final IndicatorRecorder indicatorRecorder;
    private final UserIdStripeLock userIdStripeLock;

    public UserVideoRevocationService(
            WorkspacePaths workspacePaths,
            UserVideoRepository userVideoRepository,
            IndicatorRecorder indicatorRecorder,
            UserIdStripeLock userIdStripeLock) {
        this.workspacePaths = workspacePaths;
        this.userVideoRepository = userVideoRepository;
        this.indicatorRecorder = indicatorRecorder;
        this.userIdStripeLock = userIdStripeLock;
    }

    @Transactional
    public void revokeIfOwned(String userId, String videoId) {
        synchronized (userIdStripeLock.lockFor(userId)) {
            UserVideoEntity e = userVideoRepository
                    .findById(videoId)
                    .filter(v -> userId.equals(v.getUserId()))
                    .orElseThrow(() -> new UserVideoNotFoundException(videoId));
            Path videoDir = workspacePaths.dataDirectory().resolve("videos");
            Path desubDir = workspacePaths.dataDirectory().resolve("desubtitle");
            try {
                Optional<Path> src = VideoStorageFileNames.safeResolve(videoDir, e.getStoredFileName());
                if (src.isPresent()) {
                    Files.deleteIfExists(src.get());
                }
                String outName = e.getDesubtitleOutputFileName();
                if (outName != null && !outName.isBlank()) {
                    Optional<Path> out = VideoStorageFileNames.safeResolve(desubDir, outName);
                    if (out.isPresent()) {
                        Files.deleteIfExists(out.get());
                    }
                }
            } catch (IOException ex) {
                throw new IllegalStateException("删除视频文件失败: " + ex.getMessage(), ex);
            }
            userVideoRepository.delete(e);
            indicatorRecorder.incrementCounter(
                    METRIC_REVOKES_USER_PREFIX + VideoUploadService.metricSuffixForUser(userId));
        }
    }
}
