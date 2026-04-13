package com.kitepromiss.desubtitle.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kitepromiss.desubtitle.credential.MissingAliyunCredentialsException;
import com.kitepromiss.desubtitle.user.AnonymousUserRequestAttributes;
import com.kitepromiss.desubtitle.video.SendToDeSubtitleService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class SendToDeSubtitleController {

    private final SendToDeSubtitleService sendToDeSubtitleService;

    public SendToDeSubtitleController(SendToDeSubtitleService sendToDeSubtitleService) {
        this.sendToDeSubtitleService = sendToDeSubtitleService;
    }

    @PostMapping(value = "/sendToDeSubtitle", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sendToDeSubtitle(HttpServletRequest request) {
        Object uidObj = request.getAttribute(AnonymousUserRequestAttributes.ANONYMOUS_USER_ID);
        if (!(uidObj instanceof String userId) || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "missing_user_context"));
        }
        try {
            return ResponseEntity.ok(sendToDeSubtitleService.sendAllForUser(userId));
        } catch (MissingAliyunCredentialsException e) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "need_credentials"));
        }
    }

    @PostMapping(
            value = "/sendVideoToDeSubtitle",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sendVideoToDeSubtitle(
            @RequestBody SendVideoToDeSubtitleRequest body, HttpServletRequest request) {
        Object uidObj = request.getAttribute(AnonymousUserRequestAttributes.ANONYMOUS_USER_ID);
        if (!(uidObj instanceof String userId) || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "missing_user_context"));
        }
        if (body == null || body.videoId() == null || body.videoId().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "missing_video_id"));
        }
        try {
            SubtitleVerticalPosition pos = SubtitleVerticalPosition.fromRequest(body.subtitlePosition());
            return ResponseEntity.ok(
                    sendToDeSubtitleService.sendSingleForUser(userId, body.videoId().trim(), pos));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "invalid_subtitle_position"));
        } catch (MissingAliyunCredentialsException e) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "need_credentials"));
        }
    }
}
