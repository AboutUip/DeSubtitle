package com.kitepromiss.desubtitle.video;

/** 当前用户已达 {@code config/lua/video_upload.lua} 中配置的每用户视频上限。 */
public class VideoQuotaExceededException extends RuntimeException {

    public VideoQuotaExceededException() {
        super("video_quota_exceeded");
    }
}
