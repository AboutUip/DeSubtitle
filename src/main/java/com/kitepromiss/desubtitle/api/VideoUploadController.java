package com.kitepromiss.desubtitle.api;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kitepromiss.desubtitle.user.AnonymousUserRequestAttributes;
import com.kitepromiss.desubtitle.video.VideoQuotaExceededException;
import com.kitepromiss.desubtitle.video.VideoUploadService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class VideoUploadController {

    private final VideoUploadService videoUploadService;

    public VideoUploadController(VideoUploadService videoUploadService) {
        this.videoUploadService = videoUploadService;
    }

    @PostMapping(value = "/uploadVideo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Object uidObj = request.getAttribute(AnonymousUserRequestAttributes.ANONYMOUS_USER_ID);
        if (!(uidObj instanceof String userId) || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "missing_user_context"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "empty_file"));
        }
        try {
            return ResponseEntity.ok(videoUploadService.store(file, userId));
        } catch (VideoQuotaExceededException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "video_quota_exceeded"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "upload_failed"));
        }
    }
}
