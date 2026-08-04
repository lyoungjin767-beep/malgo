package com.example.project.dto;

// 위험 표현 응답 dto
public record CultureWarningResponse(
        String expression,
        String category,
        String riskLevel,
        String reason,
        String alternativeExpression,
        Integer startIndex,
        Integer endIndex
) {
}