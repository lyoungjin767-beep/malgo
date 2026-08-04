package com.example.project.dto;

import jakarta.validation.constraints.NotBlank;

// 요청 DTO
public record TranslationRequest(
        @NotBlank(message = "원문은 필수입니다.")
        String originalText,

        @NotBlank(message = "원문 언어는 필수입니다.")
        String sourceLanguage,

        @NotBlank(message = "번역 언어는 필수입니다.")
        String targetLanguage,

        @NotBlank(message = "대상 국가는 필수입니다.")
        String targetCountry,

        @NotBlank(message = "상황은 필수입니다.")
        String situation,

        String relationshipType,
        String communicationPurpose,
        String requestedTone
) {
}