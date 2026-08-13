package com.malgo.backend.dto;

import java.util.Map;

// 마이페이지에서 번역 상황별 사용 비율을 보여주기 위한 응답 DTO

public record TranslationStatisticsResponse(
        long totalCount,
        Map<String, Long> situations,
        Map<String, Double> percentages
) {
}