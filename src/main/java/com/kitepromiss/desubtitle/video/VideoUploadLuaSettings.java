package com.kitepromiss.desubtitle.video;

import java.nio.file.Files;
import java.nio.file.Path;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.kitepromiss.desubtitle.config.LuaConfigLoader;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

/**
 * 读取 {@code config/lua/video_upload.lua} 中的上传上限、保留时长及去字幕轮询/产物保留等项。
 */
@Service
@Profile("!video-upload-quota-one")
public class VideoUploadLuaSettings {

    private static final Logger log = LoggerFactory.getLogger(VideoUploadLuaSettings.class);

    public static final String LUA_KEY_MAX_VIDEOS_PER_USER = "max_videos_per_user";
    public static final String LUA_KEY_VIDEO_RETENTION_MINUTES = "video_retention_minutes";
    public static final String LUA_KEY_DESUBTITLE_OUTPUT_RETENTION_MINUTES = "desubtitle_output_retention_minutes";
    public static final String LUA_KEY_DESUBTITLE_POLL_TIMEOUT_MINUTES = "desubtitle_poll_timeout_minutes";

    public static final int DEFAULT_MAX_VIDEOS_PER_USER = 3;
    public static final int MIN_MAX_VIDEOS_PER_USER = 1;
    public static final int MAX_MAX_VIDEOS_PER_USER = 10_000;

    public static final int DEFAULT_VIDEO_RETENTION_MINUTES = 5;
    public static final int MIN_VIDEO_RETENTION_MINUTES = 1;
    public static final int MAX_VIDEO_RETENTION_MINUTES = 10_080;

    public static final int DEFAULT_DESUBTITLE_OUTPUT_RETENTION_MINUTES = 10;
    public static final int MIN_DESUBTITLE_OUTPUT_RETENTION_MINUTES = 1;
    public static final int MAX_DESUBTITLE_OUTPUT_RETENTION_MINUTES = 10_080;

    public static final int DEFAULT_DESUBTITLE_POLL_TIMEOUT_MINUTES = 10;
    public static final int MIN_DESUBTITLE_POLL_TIMEOUT_MINUTES = 1;
    public static final int MAX_DESUBTITLE_POLL_TIMEOUT_MINUTES = 180;

    private final WorkspacePaths workspacePaths;

    public VideoUploadLuaSettings(WorkspacePaths workspacePaths) {
        this.workspacePaths = workspacePaths;
    }

    /**
     * @return 夹在 [{@value #MIN_MAX_VIDEOS_PER_USER}, {@value #MAX_MAX_VIDEOS_PER_USER}] 内；文件缺失或解析失败时返回
     *         {@value #DEFAULT_MAX_VIDEOS_PER_USER}
     */
    public int maxVideosPerUser() {
        Path p = workspacePaths.videoUploadLua();
        if (!Files.isRegularFile(p)) {
            return DEFAULT_MAX_VIDEOS_PER_USER;
        }
        try {
            LuaTable t = LuaConfigLoader.loadAsTable(p);
            LuaValue v = t.get(LUA_KEY_MAX_VIDEOS_PER_USER);
            if (v.isnil() || !v.isnumber()) {
                return DEFAULT_MAX_VIDEOS_PER_USER;
            }
            int n = (int) Math.round(v.todouble());
            return Math.min(MAX_MAX_VIDEOS_PER_USER, Math.max(MIN_MAX_VIDEOS_PER_USER, n));
        } catch (Exception e) {
            log.warn(
                    "读取 video_upload.lua 失败，使用默认每用户上限 {}: {}",
                    DEFAULT_MAX_VIDEOS_PER_USER,
                    e.toString());
            return DEFAULT_MAX_VIDEOS_PER_USER;
        }
    }

    /**
     * @return 上传文件在 {@code data/videos/} 中保留的分钟数，超时后由 {@link VideoLifecycleRecorder} 删除；文件缺失或解析失败时
     *         返回 {@value #DEFAULT_VIDEO_RETENTION_MINUTES}，数值夹到 [{@value #MIN_VIDEO_RETENTION_MINUTES},
     *         {@value #MAX_VIDEO_RETENTION_MINUTES}]。
     */
    public int videoRetentionMinutes() {
        Path p = workspacePaths.videoUploadLua();
        if (!Files.isRegularFile(p)) {
            return DEFAULT_VIDEO_RETENTION_MINUTES;
        }
        try {
            LuaTable t = LuaConfigLoader.loadAsTable(p);
            LuaValue v = t.get(LUA_KEY_VIDEO_RETENTION_MINUTES);
            if (v.isnil() || !v.isnumber()) {
                return DEFAULT_VIDEO_RETENTION_MINUTES;
            }
            int n = (int) Math.round(v.todouble());
            return Math.min(MAX_VIDEO_RETENTION_MINUTES, Math.max(MIN_VIDEO_RETENTION_MINUTES, n));
        } catch (Exception e) {
            log.warn(
                    "读取 video_upload.lua 中 retention 失败，使用默认保留 {} 分钟: {}",
                    DEFAULT_VIDEO_RETENTION_MINUTES,
                    e.toString());
            return DEFAULT_VIDEO_RETENTION_MINUTES;
        }
    }

    /**
     * 去字幕结果写入 {@code data/desubtitle/} 后的本地保留分钟数，到期后清空列并删文件。
     */
    public int desubtitleOutputRetentionMinutes() {
        Path p = workspacePaths.videoUploadLua();
        if (!Files.isRegularFile(p)) {
            return DEFAULT_DESUBTITLE_OUTPUT_RETENTION_MINUTES;
        }
        try {
            LuaTable t = LuaConfigLoader.loadAsTable(p);
            LuaValue v = t.get(LUA_KEY_DESUBTITLE_OUTPUT_RETENTION_MINUTES);
            if (v.isnil() || !v.isnumber()) {
                return DEFAULT_DESUBTITLE_OUTPUT_RETENTION_MINUTES;
            }
            int n = (int) Math.round(v.todouble());
            return Math.min(
                    MAX_DESUBTITLE_OUTPUT_RETENTION_MINUTES,
                    Math.max(MIN_DESUBTITLE_OUTPUT_RETENTION_MINUTES, n));
        } catch (Exception e) {
            log.warn(
                    "读取 video_upload.lua 中 desubtitle_output_retention_minutes 失败，使用默认 {} 分钟: {}",
                    DEFAULT_DESUBTITLE_OUTPUT_RETENTION_MINUTES,
                    e.toString());
            return DEFAULT_DESUBTITLE_OUTPUT_RETENTION_MINUTES;
        }
    }

    /**
     * 单次 {@code POST /sendToDeSubtitle} 内对单条视频轮询 GetAsyncJobResult 的最长等待（分钟）。
     */
    public int desubtitlePollTimeoutMinutes() {
        Path p = workspacePaths.videoUploadLua();
        if (!Files.isRegularFile(p)) {
            return DEFAULT_DESUBTITLE_POLL_TIMEOUT_MINUTES;
        }
        try {
            LuaTable t = LuaConfigLoader.loadAsTable(p);
            LuaValue v = t.get(LUA_KEY_DESUBTITLE_POLL_TIMEOUT_MINUTES);
            if (v.isnil() || !v.isnumber()) {
                return DEFAULT_DESUBTITLE_POLL_TIMEOUT_MINUTES;
            }
            int n = (int) Math.round(v.todouble());
            return Math.min(
                    MAX_DESUBTITLE_POLL_TIMEOUT_MINUTES,
                    Math.max(MIN_DESUBTITLE_POLL_TIMEOUT_MINUTES, n));
        } catch (Exception e) {
            log.warn(
                    "读取 video_upload.lua 中 desubtitle_poll_timeout_minutes 失败，使用默认 {} 分钟: {}",
                    DEFAULT_DESUBTITLE_POLL_TIMEOUT_MINUTES,
                    e.toString());
            return DEFAULT_DESUBTITLE_POLL_TIMEOUT_MINUTES;
        }
    }
}
