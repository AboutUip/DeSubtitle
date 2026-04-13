package com.kitepromiss.desubtitle.video;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

class VideoUploadLuaSettingsTest {

    @Test
    void readsMaxFromLua(@TempDir Path temp) throws Exception {
        Path lua = temp.resolve("video_upload.lua");
        Files.writeString(lua, "return { max_videos_per_user = 7 }\n");
        WorkspacePaths paths = new WorkspacePaths(
                temp.resolve("r.json"),
                temp.resolve("m.lua"),
                temp.resolve("data"),
                temp.resolve("a.json"),
                temp.resolve("ut.lua"),
                lua);
        assertEquals(7, new VideoUploadLuaSettings(paths).maxVideosPerUser());
    }

    @Test
    void clampsHighValue(@TempDir Path temp) throws Exception {
        Path lua = temp.resolve("video_upload.lua");
        Files.writeString(lua, "return { max_videos_per_user = 999999 }\n");
        WorkspacePaths paths = new WorkspacePaths(
                temp.resolve("r.json"),
                temp.resolve("m.lua"),
                temp.resolve("data"),
                temp.resolve("a.json"),
                temp.resolve("ut.lua"),
                lua);
        assertEquals(
                VideoUploadLuaSettings.MAX_MAX_VIDEOS_PER_USER,
                new VideoUploadLuaSettings(paths).maxVideosPerUser());
    }

    @Test
    void readsRetentionFromLua(@TempDir Path temp) throws Exception {
        Path lua = temp.resolve("video_upload.lua");
        Files.writeString(lua, "return { max_videos_per_user = 3, video_retention_minutes = 12 }\n");
        WorkspacePaths paths = pathsWithLua(temp, lua);
        assertEquals(12, new VideoUploadLuaSettings(paths).videoRetentionMinutes());
    }

    @Test
    void missingFileUsesDefault(@TempDir Path temp) {
        WorkspacePaths paths = new WorkspacePaths(
                temp.resolve("r.json"),
                temp.resolve("m.lua"),
                temp.resolve("data"),
                temp.resolve("a.json"),
                temp.resolve("ut.lua"),
                temp.resolve("nope.lua"));
        VideoUploadLuaSettings s = new VideoUploadLuaSettings(paths);
        assertEquals(VideoUploadLuaSettings.DEFAULT_MAX_VIDEOS_PER_USER, s.maxVideosPerUser());
        assertEquals(VideoUploadLuaSettings.DEFAULT_VIDEO_RETENTION_MINUTES, s.videoRetentionMinutes());
        assertEquals(
                VideoUploadLuaSettings.DEFAULT_DESUBTITLE_OUTPUT_RETENTION_MINUTES,
                s.desubtitleOutputRetentionMinutes());
        assertEquals(
                VideoUploadLuaSettings.DEFAULT_DESUBTITLE_POLL_TIMEOUT_MINUTES,
                s.desubtitlePollTimeoutMinutes());
    }

    @Test
    void readsDesubtitleSettingsFromLua(@TempDir Path temp) throws Exception {
        Path lua = temp.resolve("video_upload.lua");
        Files.writeString(
                lua,
                "return { max_videos_per_user = 3, desubtitle_output_retention_minutes = 7, "
                        + "desubtitle_poll_timeout_minutes = 4 }\n");
        WorkspacePaths paths = pathsWithLua(temp, lua);
        VideoUploadLuaSettings s = new VideoUploadLuaSettings(paths);
        assertEquals(7, s.desubtitleOutputRetentionMinutes());
        assertEquals(4, s.desubtitlePollTimeoutMinutes());
    }

    private static WorkspacePaths pathsWithLua(Path temp, Path lua) {
        return new WorkspacePaths(
                temp.resolve("r.json"),
                temp.resolve("m.lua"),
                temp.resolve("data"),
                temp.resolve("a.json"),
                temp.resolve("ut.lua"),
                lua);
    }
}
