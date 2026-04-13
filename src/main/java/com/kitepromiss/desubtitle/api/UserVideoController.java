package com.kitepromiss.desubtitle.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.kitepromiss.desubtitle.indicator.VideoLifecycleDetail;
import com.kitepromiss.desubtitle.user.AnonymousUserRequestAttributes;
import com.kitepromiss.desubtitle.video.UserVideoRevocationService;
import com.kitepromiss.desubtitle.video.VideoLifecycleRecorder;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 当前 JWT 用户维度的视频列表与主动撤销。
 */
@RestController
public class UserVideoController {

    private final VideoLifecycleRecorder videoLifecycleRecorder;
    private final UserVideoRevocationService userVideoRevocationService;

    public UserVideoController(
            VideoLifecycleRecorder videoLifecycleRecorder,
            UserVideoRevocationService userVideoRevocationService) {
        this.videoLifecycleRecorder = videoLifecycleRecorder;
        this.userVideoRevocationService = userVideoRevocationService;
    }

    /** 返回当前用户上传视频的生命周期明细（与指标中 {@link VideoLifecycleDetail} 结构一致）。 */
    @GetMapping(value = "/myVideos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> myVideos(HttpServletRequest request) {
        String userId = currentUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "missing_user_context"));
        }
        List<VideoLifecycleDetail> list =
                videoLifecycleRecorder.videoLifecyclesForUserAtMillis(userId, System.currentTimeMillis());
        return ResponseEntity.ok(list);
    }

    /** 撤销指定视频（仅所有者）；成功返回 204。 */
    @DeleteMapping("/userVideo/{videoId}")
    public ResponseEntity<?> revokeVideo(@PathVariable("videoId") String videoId, HttpServletRequest request) {
        String userId = currentUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "missing_user_context"));
        }
        userVideoRevocationService.revokeIfOwned(userId, videoId);
        return ResponseEntity.noContent().build();
    }

    private static String currentUserId(HttpServletRequest request) {
        Object uidObj = request.getAttribute(AnonymousUserRequestAttributes.ANONYMOUS_USER_ID);
        if (!(uidObj instanceof String userId) || userId.isBlank()) {
            return null;
        }
        return userId;
    }
}
