package com.kitepromiss.desubtitle.api;

import java.io.IOException;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.kitepromiss.desubtitle.user.AnonymousUserRequestAttributes;
import com.kitepromiss.desubtitle.video.UserVideoStreamService;
import com.kitepromiss.desubtitle.video.UserVideoStreamService.UserVideoStreamTarget;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 当前用户源视频与去字幕成品的只读流（须 Bearer）；供前端 {@code fetch}+Blob 绑定 {@code video}（HTML 无法为 {@code src} 带头）。
 */
@RestController
public class UserVideoStreamController {

    private final UserVideoStreamService userVideoStreamService;

    public UserVideoStreamController(UserVideoStreamService userVideoStreamService) {
        this.userVideoStreamService = userVideoStreamService;
    }

    @GetMapping("/userVideo/{videoId}/source")
    public ResponseEntity<?> streamSource(@PathVariable("videoId") String videoId, HttpServletRequest request)
            throws IOException {
        String userId = currentUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "missing_user_context"));
        }
        UserVideoStreamTarget t = userVideoStreamService.resolveSource(userId, videoId);
        return streamResponse(t);
    }

    @GetMapping("/userVideo/{videoId}/output")
    public ResponseEntity<?> streamOutput(@PathVariable("videoId") String videoId, HttpServletRequest request)
            throws IOException {
        String userId = currentUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "missing_user_context"));
        }
        UserVideoStreamTarget t = userVideoStreamService.resolveOutput(userId, videoId);
        return streamResponse(t);
    }

    private static ResponseEntity<Resource> streamResponse(UserVideoStreamTarget t) throws IOException {
        FileSystemResource body = new FileSystemResource(t.path());
        long len = body.contentLength();
        return ResponseEntity.ok()
                .contentType(t.mediaType())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentLength(len)
                .body(body);
    }

    private static String currentUserId(HttpServletRequest request) {
        Object uidObj = request.getAttribute(AnonymousUserRequestAttributes.ANONYMOUS_USER_ID);
        if (!(uidObj instanceof String userId) || userId.isBlank()) {
            return null;
        }
        return userId;
    }
}
