package com.kitepromiss.desubtitle.video;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

class VideoLifecycleRecorderTest {

    @Test
    void secondsUntilExpiryFloorsToWholeSeconds() {
        long nowMs = 1_000_000_000_000L;
        Instant created = Instant.ofEpochMilli(nowMs - 10_000);
        Instant expires = Instant.ofEpochMilli(nowMs + 45_500);
        UserVideoEntity e =
                new UserVideoEntity("vid-1", "u1", "a.mp4", "a.mp4", "video/mp4", 9, created, expires);
        UserVideoRepository repo = repoFindAllReturns(List.of(e));
        WorkspacePaths paths =
                new WorkspacePaths(
                        Path.of("r.json"),
                        Path.of("m.lua"),
                        Path.of("data"),
                        Path.of("a.json"),
                        Path.of("ut.lua"),
                        Path.of("vu.lua"));
        VideoLifecycleRecorder recorder = new VideoLifecycleRecorder(repo, paths);
        Map<String, Long> m = recorder.secondsUntilExpiryAtMillis(nowMs);
        assertEquals(45L, m.get("vid-1").longValue());
    }

    @Test
    void expiredShowsZeroUntilPurge() {
        long nowMs = 2_000_000_000_000L;
        Instant created = Instant.ofEpochMilli(nowMs - 120_000);
        Instant expires = Instant.ofEpochMilli(nowMs - 1);
        UserVideoEntity e =
                new UserVideoEntity("vid-2", "u1", "b.mp4", null, null, 1, created, expires);
        UserVideoRepository repo = repoFindAllReturns(List.of(e));
        WorkspacePaths paths =
                new WorkspacePaths(
                        Path.of("r.json"),
                        Path.of("m.lua"),
                        Path.of("data"),
                        Path.of("a.json"),
                        Path.of("ut.lua"),
                        Path.of("vu.lua"));
        VideoLifecycleRecorder recorder = new VideoLifecycleRecorder(repo, paths);
        assertEquals(0L, recorder.secondsUntilExpiryAtMillis(nowMs).get("vid-2").longValue());
    }

    @SuppressWarnings("unchecked")
    private static UserVideoRepository repoFindAllReturns(List<UserVideoEntity> rows) {
        return (UserVideoRepository)
                Proxy.newProxyInstance(
                        UserVideoRepository.class.getClassLoader(),
                        new Class<?>[] {UserVideoRepository.class},
                        (p, m, a) -> {
                            if ("findAll".equals(m.getName())) {
                                return rows;
                            }
                            if ("findByExpiresAtBefore".equals(m.getName())) {
                                return List.of();
                            }
                            if ("findOutputExpiredBefore".equals(m.getName())) {
                                return List.of();
                            }
                            if ("findByUserIdOrderByCreatedAtAsc".equals(m.getName())) {
                                return List.of();
                            }
                            if (m.getReturnType() == long.class || m.getReturnType() == Long.class) {
                                return 0L;
                            }
                            if (m.getReturnType() == boolean.class) {
                                return false;
                            }
                            return null;
                        });
    }
}
