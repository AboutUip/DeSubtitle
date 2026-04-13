package com.kitepromiss.desubtitle.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;

import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

class UserVideoStreamServiceTest {

    @Test
    void resolveSource(@TempDir Path temp) throws Exception {
        Path data = temp.resolve("data");
        Path videos = data.resolve("videos");
        Files.createDirectories(videos);
        String stored = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11.mp4";
        Files.writeString(videos.resolve(stored), "fake");
        UserVideoEntity e =
                new UserVideoEntity(
                        "vid-1",
                        "u1",
                        stored,
                        "x.mp4",
                        "video/mp4",
                        4,
                        Instant.now(),
                        Instant.now().plusSeconds(3600));
        UserVideoRepository repo = repoFindByIdOnly(e);
        UserVideoStreamService svc = new UserVideoStreamService(repo, dummyPaths(temp));
        UserVideoStreamService.UserVideoStreamTarget t = svc.resolveSource("u1", "vid-1");
        assertTrue(Files.isSameFile(t.path(), videos.resolve(stored)));
        assertEquals(MediaType.parseMediaType("video/mp4"), t.mediaType());
    }

    @Test
    void resolveOutput(@TempDir Path temp) throws Exception {
        Path data = temp.resolve("data");
        Path desub = data.resolve("desubtitle");
        Files.createDirectories(desub);
        String out = "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22.mp4";
        Files.writeString(desub.resolve(out), "out");
        UserVideoEntity e =
                new UserVideoEntity(
                        "vid-1",
                        "u1",
                        "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11.mp4",
                        "x.mp4",
                        "video/mp4",
                        4,
                        Instant.now(),
                        Instant.now().plusSeconds(3600));
        e.setDesubtitleOutputFileName(out);
        UserVideoStreamService svc = new UserVideoStreamService(repoFindByIdOnly(e), dummyPaths(temp));
        UserVideoStreamService.UserVideoStreamTarget t = svc.resolveOutput("u1", "vid-1");
        assertTrue(Files.isSameFile(t.path(), desub.resolve(out)));
    }

    @Test
    void wrongUserThrows(@TempDir Path temp) {
        UserVideoEntity e =
                new UserVideoEntity(
                        "vid-1",
                        "u1",
                        "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11.mp4",
                        "x.mp4",
                        "video/mp4",
                        4,
                        Instant.now(),
                        Instant.now().plusSeconds(3600));
        UserVideoStreamService svc = new UserVideoStreamService(repoFindByIdOnly(e), dummyPaths(temp));
        assertThrows(UserVideoNotFoundException.class, () -> svc.resolveSource("other", "vid-1"));
    }

    @SuppressWarnings("unchecked")
    private static UserVideoRepository repoFindByIdOnly(UserVideoEntity e) {
        return (UserVideoRepository)
                Proxy.newProxyInstance(
                        UserVideoRepository.class.getClassLoader(),
                        new Class<?>[] {UserVideoRepository.class},
                        (p, m, a) -> {
                            if ("findById".equals(m.getName())) {
                                return Optional.of(e).filter(x -> x.getId().equals(a[0]));
                            }
                            if (m.getReturnType() == long.class || m.getReturnType() == Long.class) {
                                return 0L;
                            }
                            if (List.class.isAssignableFrom(m.getReturnType())) {
                                return List.of();
                            }
                            return null;
                        });
    }

    private static WorkspacePaths dummyPaths(Path temp) {
        return new WorkspacePaths(
                temp.resolve("r.json"),
                temp.resolve("m.lua"),
                temp.resolve("data"),
                temp.resolve("a.json"),
                temp.resolve("ut.lua"),
                temp.resolve("vu.lua"));
    }
}
