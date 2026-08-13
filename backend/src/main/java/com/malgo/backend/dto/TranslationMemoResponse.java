package com.malgo.backend.dto;

import java.time.LocalDateTime;

// 번역 기록 메모 응답 DTO

public record TranslationMemoResponse(
        Long id,
        Long translationId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}