package com.malgo.backend.dto;

import jakarta.validation.constraints.NotBlank;

// 번역 기록 메모 저장/수정 요청 DTO

public record TranslationMemoRequest(
        @NotBlank(message = "메모 내용은 필수입니다.")
        String content
) {
}