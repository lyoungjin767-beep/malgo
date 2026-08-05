package com.malgo.backend.dto;

import java.util.List;

    // 번역 상세 조회 응답 DTO

    // 번역 기록 하나를 클릭했을 때 원문 + 번역 결과 + 말투 점수 + 문화적 경고를 모두 반환

public record TranslationDetailResponse(

        Long id,

        String originalText,

        String literalTranslation,

        String naturalTranslation,

        String culturalTranslation,

        String culturalExplanation,

        String overallRiskLevel,

        ToneScores toneScores,

        List<CultureWarningResponse> warnings

) {
}