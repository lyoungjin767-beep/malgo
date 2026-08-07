package com.malgo.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;


// 프로젝트 전체에서 발생하는 예외를 공통으로 처리
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 번역 기록을 찾을 수 없는 경우 404 Not Found 응답을 반환

    @ExceptionHandler(TranslationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTranslationNotFound(
            TranslationNotFoundException e
    ) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 404,
                "error", "Not Found",
                "message", e.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(body);
    }
}