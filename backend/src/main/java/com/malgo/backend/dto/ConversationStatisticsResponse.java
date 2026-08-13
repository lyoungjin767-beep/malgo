package com.malgo.backend.dto;

import java.util.Map;

//마이페이지 분야별 대화 분석 응답 DTO

public record ConversationStatisticsResponse(
        long totalCount,
        Map<String, Long> counts,
        Map<String, Double> percentages
) {
}