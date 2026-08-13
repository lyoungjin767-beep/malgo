package com.malgo.backend.dto;

//마이페이지의 최근 번역 기록에 표시할 데이터

import java.time.LocalDateTime;

public record MyPageTranslationResponse(
        Long translationId,
        String originalText,
        String recommendedTranslation,
        LocalDateTime createdAt,
        boolean hasMemo
) {
}