package com.kitepromiss.desubtitle.video;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

/**
 * 校验当前用户所有权后，解析 {@code data/videos/} 源文件或 {@code data/desubtitle/} 成品路径，供 HTTP 流式返回。
 */
@Service
public class UserVideoStreamService {

    private final UserVideoRepository userVideoRepository;
    private final WorkspacePaths workspacePaths;

    public UserVideoStreamService(UserVideoRepository userVideoRepository, WorkspacePaths workspacePaths) {
        this.userVideoRepository = userVideoRepository;
        this.workspacePaths = workspacePaths;
    }

    /**
     * @throws UserVideoNotFoundException 无行、非本人、路径非法或文件不存在
     */
    public UserVideoStreamTarget resolveSource(String userId, String videoId) {
        UserVideoEntity e = ownedEntity(userId, videoId);
        Path videosDir = workspacePaths.dataDirectory().resolve("videos");
        Optional<Path> path = VideoStorageFileNames.safeResolve(videosDir, e.getStoredFileName());
        if (path.isEmpty() || !Files.isRegularFile(path.get())) {
            throw new UserVideoNotFoundException(videoId);
        }
        return new UserVideoStreamTarget(path.get(), mediaTypeForSource(e.getContentType()));
    }

    /**
     * @throws UserVideoNotFoundException 无成品名、路径非法或文件不存在
     */
    public UserVideoStreamTarget resolveOutput(String userId, String videoId) {
        UserVideoEntity e = ownedEntity(userId, videoId);
        String out = e.getDesubtitleOutputFileName();
        if (out == null || out.isBlank()) {
            throw new UserVideoNotFoundException(videoId);
        }
        Path desubDir = workspacePaths.dataDirectory().resolve("desubtitle");
        Optional<Path> path = VideoStorageFileNames.safeResolve(desubDir, out);
        if (path.isEmpty() || !Files.isRegularFile(path.get())) {
            throw new UserVideoNotFoundException(videoId);
        }
        return new UserVideoStreamTarget(path.get(), mediaTypeForPath(path.get()));
    }

    private UserVideoEntity ownedEntity(String userId, String videoId) {
        return userVideoRepository
                .findById(videoId)
                .filter(v -> userId.equals(v.getUserId()))
                .orElseThrow(() -> new UserVideoNotFoundException(videoId));
    }

    private static MediaType mediaTypeForSource(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType.trim());
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static MediaType mediaTypeForPath(Path path) {
        try {
            String probed = Files.probeContentType(path);
            if (probed != null && !probed.isBlank()) {
                return MediaType.parseMediaType(probed);
            }
        } catch (IOException | InvalidMediaTypeException ignored) {
            // fall through
        }
        return guessVideoTypeByName(path.getFileName().toString());
    }

    private static MediaType guessVideoTypeByName(String fileName) {
        String n = fileName.toLowerCase();
        if (n.endsWith(".mp4")) {
            return MediaType.parseMediaType("video/mp4");
        }
        if (n.endsWith(".webm")) {
            return MediaType.parseMediaType("video/webm");
        }
        if (n.endsWith(".mov")) {
            return MediaType.parseMediaType("video/quicktime");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    /** 已校验存在且可读的流式目标。 */
    public record UserVideoStreamTarget(Path path, MediaType mediaType) {}
}
