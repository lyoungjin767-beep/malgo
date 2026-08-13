package com.malgo.backend.dto;

// 분위기 점수 dto
public record ToneScores(
        int friendliness,
        int politeness,
        int directness,
        int aggression,
        int burden,
        int professionalism,
        int naturalness
) {
}