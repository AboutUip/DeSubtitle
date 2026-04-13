package com.kitepromiss.desubtitle.video;

/** 视频 id 不存在或不属于当前用户时由业务层抛出，由全局异常处理映射为 HTTP 404。 */
public final class UserVideoNotFoundException extends RuntimeException {

    private final String videoId;

    public UserVideoNotFoundException(String videoId) {
        super("video_not_found");
        this.videoId = videoId;
    }

    public String videoId() {
        return videoId;
    }
}
