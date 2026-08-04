package com.malgo.backend.dto;

import java.util.List;

// 전체 응답 dto
public record TranslationResponse(
        String literalTranslation,
        String naturalTranslation,
        String culturalTranslation,
        String culturalExplanation,
        String overallRiskLevel,
        ToneScores toneScores,
        List<CultureWarningResponse> warnings
) {
}