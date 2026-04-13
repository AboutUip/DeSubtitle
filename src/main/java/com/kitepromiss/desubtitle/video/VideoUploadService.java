package com.kitepromiss.desubtitle.video;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kitepromiss.desubtitle.api.UploadVideoResponse;
import com.kitepromiss.desubtitle.indicator.IndicatorRecorder;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

@Service
public class VideoUploadService {

    /** 指标计数器名前缀，后接经 {@link #metricSuffixForUser(String)} 处理的 userId。 */
    public static final String METRIC_COUNTER_PREFIX = "video_uploads_user_";

    private static final int STRIPE_COUNT = 64;
    private final Object[] uploadStripes = new Object[STRIPE_COUNT];

    private final WorkspacePaths workspacePaths;
    private final UserVideoRepository userVideoRepository;
    private final VideoUploadLuaSettings videoUploadLuaSettings;
    private final IndicatorRecorder indicatorRecorder;

    public VideoUploadService(
            WorkspacePaths workspacePaths,
            UserVideoRepository userVideoRepository,
            VideoUploadLuaSettings videoUploadLuaSettings,
            IndicatorRecorder indicatorRecorder) {
        this.workspacePaths = workspacePaths;
        this.userVideoRepository = userVideoRepository;
        this.videoUploadLuaSettings = videoUploadLuaSettings;
        this.indicatorRecorder = indicatorRecorder;
        for (int i = 0; i < STRIPE_COUNT; i++) {
            uploadStripes[i] = new Object();
        }
    }

    @Transactional
    public UploadVideoResponse store(MultipartFile file, String userId) throws IOException {
        Object stripe = uploadStripes[Math.floorMod(userId.hashCode(), STRIPE_COUNT)];
        synchronized (stripe) {
            return storeUnderStripe(file, userId);
        }
    }

    private UploadVideoResponse storeUnderStripe(MultipartFile file, String userId) throws IOException {
        long existing = userVideoRepository.countByUserId(userId);
        int max = videoUploadLuaSettings.maxVideosPerUser();
        if (existing >= max) {
            throw new VideoQuotaExceededException();
        }

        Path dir = workspacePaths.dataDirectory().resolve("videos");
        Files.createDirectories(dir);

        String ext = safeExtension(file.getOriginalFilename());
        String storedName;
        Path target;
        do {
            storedName = UUID.randomUUID() + ext;
            target = dir.resolve(storedName);
        } while (Files.exists(target));

        String id = UUID.randomUUID().toString();
        String original = truncateOriginal(file.getOriginalFilename());
        String contentType = truncateContentType(file.getContentType());
        long size = file.getSize();

        Instant created = Instant.now();
        Instant expiresAt = created.plusSeconds((long) videoUploadLuaSettings.videoRetentionMinutes() * 60L);
        UserVideoEntity entity = new UserVideoEntity(id, userId, storedName, original, contentType, size, created, expiresAt);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(target);
            throw e;
        }

        try {
            userVideoRepository.save(entity);
        } catch (RuntimeException e) {
            Files.deleteIfExists(target);
            throw e;
        }

        indicatorRecorder.incrementCounter(METRIC_COUNTER_PREFIX + metricSuffixForUser(userId));
        return new UploadVideoResponse(id, storedName, original, size, contentType);
    }

    static String metricSuffixForUser(String userId) {
        return userId.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private static String truncateOriginal(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String n = name.trim();
        if (n.length() > 512) {
            return n.substring(0, 512);
        }
        return n;
    }

    private static String truncateContentType(String ct) {
        if (ct == null || ct.isBlank()) {
            return null;
        }
        String t = ct.trim();
        if (t.length() > 128) {
            return t.substring(0, 128);
        }
        return t;
    }

    static String safeExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }
        int i = originalFilename.lastIndexOf('.');
        if (i < 0 || i >= originalFilename.length() - 1) {
            return "";
        }
        String stem = originalFilename.substring(0, i);
        if (stem.contains(".")) {
            return "";
        }
        String ext = originalFilename.substring(i);
        if (ext.length() > 16) {
            return "";
        }
        if (!ext.matches("\\.[a-zA-Z0-9]+")) {
            return "";
        }
        return ext.toLowerCase();
    }
}
