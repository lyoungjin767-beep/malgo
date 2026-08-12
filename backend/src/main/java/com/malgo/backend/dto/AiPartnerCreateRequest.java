package com.malgo.backend.dto;

// AI 메이커에서 커스텀 AI 상대를 생성할 때 사용하는 요청 DTO

public record AiPartnerCreateRequest(
        String name,
        String targetCountry,
        String relationshipType,
        String ageGroup,
        String gender,
        String speechStyle,
        String characteristic
) {
}