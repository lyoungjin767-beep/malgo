package com.malgo.backend.dto;

// 기존 커스텀 AI 상대 정보를 수정할 때 사용하는 요청 DTO

public record AiPartnerUpdateRequest(
        String name,
        String targetCountry,
        String relationshipType,
        String ageGroup,
        String gender,
        String speechStyle,
        String characteristic
) {
}