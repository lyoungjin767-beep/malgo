package com.malgo.backend.dto;

import java.time.LocalDateTime;

    // 번역 기록 목록에서 한 건의 정보를 반환하는 DTO
    // 목록 화면에서는 전체 AI 분석 결과보다 원문, 언어, 국가, 생성 시간 등의 기본 정보만 전달

public record TranslationHistoryResponse(
        Long id,
        String originalText,
        String sourceLanguage,
        String targetLanguage,
        String targetCountry,
        String situation,
        LocalDateTime createdAt
) {
}