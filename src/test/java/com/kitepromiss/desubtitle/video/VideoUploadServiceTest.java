package com.kitepromiss.desubtitle.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.kitepromiss.desubtitle.api.UploadVideoResponse;
import com.kitepromiss.desubtitle.indicator.InMemoryIndicatorRegistry;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

class VideoUploadServiceTest {

    @Test
    void safeExtensionAcceptsAlphanumericSuffix() {
        assertEquals(".mp4", VideoUploadService.safeExtension("a.MP4"));
        assertEquals("", VideoUploadService.safeExtension("noext"));
        assertEquals("", VideoUploadService.safeExtension("bad.evil.exe"));
    }

    @Test
    void throwsWhenQuotaReached(@TempDir Path temp) throws Exception {
        Files.writeString(temp.resolve("video_upload.lua"), "return { max_videos_per_user = 1 }\n");
        WorkspacePaths paths = workspace(temp);
        UserVideoRepository repo = countingRepo(1L);
        VideoUploadService svc =
                new VideoUploadService(paths, repo, new VideoUploadLuaSettings(paths), new InMemoryIndicatorRegistry());
        MockMultipartFile file = new MockMultipartFile("file", "a.mp4", "video/mp4", "x".getBytes());
        assertThrows(VideoQuotaExceededException.class, () -> svc.store(file, "u1"));
    }

    @Test
    void storesUnderVideosAndIncrementsCounter(@TempDir Path temp) throws Exception {
        Files.writeString(temp.resolve("video_upload.lua"), "return { max_videos_per_user = 5 }\n");
        WorkspacePaths paths = workspace(temp);
        UserVideoRepository repo = countingRepo(0L);
        InMemoryIndicatorRegistry reg = new InMemoryIndicatorRegistry();
        VideoUploadService svc = new VideoUploadService(paths, repo, new VideoUploadLuaSettings(paths), reg);

        MockMultipartFile file = new MockMultipartFile("file", "a.mp4", "video/mp4", "hello".getBytes());
        UploadVideoResponse r = svc.store(file, "u1");

        Path stored = paths.dataDirectory().resolve("videos").resolve(r.storedFileName());
        assertTrue(Files.isRegularFile(stored));
        assertEquals("hello", Files.readString(stored));
        assertEquals(
                1L,
                reg.snapshot()
                        .counters()
                        .get(VideoUploadService.METRIC_COUNTER_PREFIX + "u1")
                        .longValue());
    }

    private static WorkspacePaths workspace(Path temp) {
        return new WorkspacePaths(
                temp.resolve("r.json"),
                temp.resolve("m.lua"),
                temp.resolve("data"),
                temp.resolve("a.json"),
                temp.resolve("ut.lua"),
                temp.resolve("video_upload.lua"));
    }

    @SuppressWarnings("unchecked")
    private static UserVideoRepository countingRepo(long count) {
        return (UserVideoRepository)
                Proxy.newProxyInstance(
                        UserVideoRepository.class.getClassLoader(),
                        new Class<?>[] {UserVideoRepository.class},
                        (p, m, a) -> {
                            if ("countByUserId".equals(m.getName())) {
                                return count;
                            }
                            if ("save".equals(m.getName())) {
                                return a[0];
                            }
                            if ("delete".equals(m.getName()) || "deleteAll".equals(m.getName())) {
                                return null;
                            }
                            if (m.getReturnType() == boolean.class) {
                                return false;
                            }
                            if (m.getReturnType().isPrimitive()) {
                                return 0;
                            }
                            return null;
                        });
    }
}
