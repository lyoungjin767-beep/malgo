package com.malgo.backend.dto;

// AI 대화 상대 목록을 프론트에 전달하기 위한 DTO

public record AiPartnerResponse(
        Long id,
        String name,
        String targetCountry,
        String relationshipType,
        String ageGroup,
        String gender,
        String speechStyle,
        String characteristic,
        boolean custom
) {
}