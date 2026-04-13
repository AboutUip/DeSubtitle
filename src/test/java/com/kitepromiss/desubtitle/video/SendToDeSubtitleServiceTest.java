package com.kitepromiss.desubtitle.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.kitepromiss.desubtitle.aliyun.SubtitleAsyncJobState;
import com.kitepromiss.desubtitle.aliyun.SubtitleEraseBand;
import com.kitepromiss.desubtitle.aliyun.VideoenhanSubtitleEraseOperations;
import com.kitepromiss.desubtitle.api.SendToDeSubtitleBatchResponse;
import com.kitepromiss.desubtitle.api.SendToDeSubtitleItemResult;
import com.kitepromiss.desubtitle.credential.AliyunCredentialsSource;
import com.kitepromiss.desubtitle.credential.ResolvedAliyunKeys;
import com.kitepromiss.desubtitle.indicator.InMemoryIndicatorRegistry;
import com.kitepromiss.desubtitle.video.UserVideoNotFoundException;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;
import com.sun.net.httpserver.HttpServer;

class SendToDeSubtitleServiceTest {

    @Test
    void submitsPollsAndDownloadsToDesubtitleDir(@TempDir Path temp) throws Exception {
        Files.writeString(
                temp.resolve("video_upload.lua"),
                "return { max_videos_per_user = 3, desubtitle_output_retention_minutes = 10, "
                        + "desubtitle_poll_timeout_minutes = 10 }\n");

        WorkspacePaths paths =
                new WorkspacePaths(
                        temp.resolve("r.json"),
                        temp.resolve("m.lua"),
                        temp.resolve("data"),
                        temp.resolve("a.json"),
                        temp.resolve("ut.lua"),
                        temp.resolve("video_upload.lua"));

        Path videos = paths.dataDirectory().resolve("videos");
        Files.createDirectories(videos);
        String storedOnDisk = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11.mp4";
        Files.writeString(videos.resolve(storedOnDisk), "src");

        Instant now = Instant.now();
        UserVideoEntity entity =
                new UserVideoEntity(
                        "vid-1",
                        "u1",
                        storedOnDisk,
                        "in.mp4",
                        "video/mp4",
                        3,
                        now,
                        now.plusSeconds(3600));

        List<UserVideoEntity> saved = new ArrayList<>();
        UserVideoRepository repo = userVideoRepoStub(entity, saved);

        HttpServer http = HttpServer.create(new InetSocketAddress(0), 0);
        http.createContext(
                "/out.mp4",
                ex -> {
                    ex.sendResponseHeaders(200, 3);
                    try (OutputStream os = ex.getResponseBody()) {
                        os.write(new byte[] {1, 2, 3});
                    }
                });
        http.start();
        int port = http.getAddress().getPort();
        try {
            String videoUrl = "http://127.0.0.1:" + port + "/out.mp4";
            VideoenhanSubtitleEraseOperations gw =
                    new VideoenhanSubtitleEraseOperations() {
                        @Override
                        public String submitEraseLocalFile(
                                Path videoFile,
                                String accessKeyId,
                                String accessKeySecret,
                                SubtitleEraseBand regionOverride) {
                            return "job-1";
                        }

                        @Override
                        public SubtitleAsyncJobState getAsyncJob(
                                String jobId, String accessKeyId, String accessKeySecret) {
                            return new SubtitleAsyncJobState(
                                    SendToDeSubtitleService.STATUS_PROCESS_SUCCESS,
                                    "{\"VideoUrl\":\"" + videoUrl + "\"}",
                                    null,
                                    null);
                        }
                    };

            AliyunCredentialsSource keys = () -> Optional.of(new ResolvedAliyunKeys("ak", "sk"));

            SendToDeSubtitleService svc =
                    new SendToDeSubtitleService(
                            paths,
                            repo,
                            new VideoUploadLuaSettings(paths),
                            new InMemoryIndicatorRegistry(),
                            new UserIdStripeLock(),
                            keys,
                            gw);

            SendToDeSubtitleBatchResponse batch = svc.sendAllForUser("u1");
            assertEquals(1, batch.results().size());
            SendToDeSubtitleItemResult r = batch.results().get(0);
            assertEquals(SendToDeSubtitleService.OUTCOME_SUCCESS, r.outcome());
            assertTrue(r.storedOutputFileName() != null && !r.storedOutputFileName().isBlank());

            Path outFile = paths.dataDirectory().resolve("desubtitle").resolve(r.storedOutputFileName());
            assertTrue(Files.isRegularFile(outFile));
            assertEquals(3, Files.size(outFile));
            assertTrue(saved.stream().anyMatch(v -> r.storedOutputFileName().equals(v.getDesubtitleOutputFileName())));
        } finally {
            http.stop(0);
        }
    }

    @Test
    void sendSingleForUserThrowsWhenVideoMissing(@TempDir Path temp) throws Exception {
        Files.writeString(
                temp.resolve("video_upload.lua"),
                "return { max_videos_per_user = 3, desubtitle_output_retention_minutes = 10, "
                        + "desubtitle_poll_timeout_minutes = 10 }\n");
        WorkspacePaths paths =
                new WorkspacePaths(
                        temp.resolve("r.json"),
                        temp.resolve("m.lua"),
                        temp.resolve("data"),
                        temp.resolve("a.json"),
                        temp.resolve("ut.lua"),
                        temp.resolve("video_upload.lua"));
        UserVideoRepository repo = emptyFindByIdRepo();
        AliyunCredentialsSource keys = () -> Optional.of(new ResolvedAliyunKeys("ak", "sk"));
        VideoenhanSubtitleEraseOperations gw =
                new VideoenhanSubtitleEraseOperations() {
                    @Override
                    public String submitEraseLocalFile(
                            Path videoFile,
                            String accessKeyId,
                            String accessKeySecret,
                            SubtitleEraseBand regionOverride) {
                        throw new AssertionError();
                    }

                    @Override
                    public SubtitleAsyncJobState getAsyncJob(
                            String jobId, String accessKeyId, String accessKeySecret) {
                        throw new AssertionError();
                    }
                };
        SendToDeSubtitleService svc =
                new SendToDeSubtitleService(
                        paths,
                        repo,
                        new VideoUploadLuaSettings(paths),
                        new InMemoryIndicatorRegistry(),
                        new UserIdStripeLock(),
                        keys,
                        gw);
        assertThrows(UserVideoNotFoundException.class, () -> svc.sendSingleForUser("u1", "nope"));
    }

    @SuppressWarnings("unchecked")
    private static UserVideoRepository emptyFindByIdRepo() {
        return (UserVideoRepository)
                Proxy.newProxyInstance(
                        UserVideoRepository.class.getClassLoader(),
                        new Class<?>[] {UserVideoRepository.class},
                        (p, m, a) -> {
                            if ("findById".equals(m.getName())) {
                                return Optional.empty();
                            }
                            if (m.getReturnType() == long.class || m.getReturnType() == Long.class) {
                                return 0L;
                            }
                            if (m.getReturnType() == boolean.class) {
                                return false;
                            }
                            if (List.class.isAssignableFrom(m.getReturnType())) {
                                return List.of();
                            }
                            if (Optional.class == m.getReturnType()) {
                                return Optional.empty();
                            }
                            return null;
                        });
    }

    @SuppressWarnings("unchecked")
    private static UserVideoRepository userVideoRepoStub(UserVideoEntity entity, List<UserVideoEntity> saved) {
        return (UserVideoRepository)
                Proxy.newProxyInstance(
                        UserVideoRepository.class.getClassLoader(),
                        new Class<?>[] {UserVideoRepository.class},
                        (p, m, a) -> {
                            if ("findByUserIdOrderByCreatedAtAsc".equals(m.getName())) {
                                return List.of(entity);
                            }
                            if ("findById".equals(m.getName())) {
                                Object id = a[0];
                                return Optional.ofNullable(
                                        id != null && id.equals(entity.getId()) ? entity : null);
                            }
                            if ("save".equals(m.getName())) {
                                saved.add((UserVideoEntity) a[0]);
                                return a[0];
                            }
                            if (m.getReturnType() == long.class || m.getReturnType() == Long.class) {
                                return 0L;
                            }
                            if (m.getReturnType() == boolean.class) {
                                return false;
                            }
                            if (m.getReturnType() == int.class || m.getReturnType() == Integer.class) {
                                return 0;
                            }
                            if (List.class.isAssignableFrom(m.getReturnType())) {
                                return List.of();
                            }
                            if (Optional.class == m.getReturnType()) {
                                return Optional.empty();
                            }
                            return null;
                        });
    }
}
