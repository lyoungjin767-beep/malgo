package com.malgo.backend.dto;

import jakarta.validation.constraints.NotBlank;

// AI 메이커에서 커스텀 AI 상대를 생성할 때 사용하는 요청 DTO

public record AiPartnerCreateRequest(
        @NotBlank(message = "AI 상대 이름은 필수입니다.")
        String name,

        @NotBlank(message = "대상 국가는 필수입니다.")
        String targetCountry,

        @NotBlank(message = "관계는 필수입니다.")
        String relationshipType,

        String ageGroup,
        String gender,
        String speechStyle,
        String characteristic
) {
}