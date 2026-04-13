package com.kitepromiss.desubtitle.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kitepromiss.desubtitle.video.UserVideoNotFoundException;

@RestControllerAdvice
public class UserVideoNotFoundHandler {

    @ExceptionHandler(UserVideoNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(UserVideoNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", "video_not_found"));
    }
}
