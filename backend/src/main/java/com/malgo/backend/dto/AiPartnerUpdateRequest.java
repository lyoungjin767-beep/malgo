package com.malgo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// 기존 커스텀 AI 상대 정보를 수정할 때 사용하는 요청 DTO

public record AiPartnerUpdateRequest(
        @NotBlank(message = "AI 상대 이름은 필수입니다.")
        String name,

        @NotBlank(message = "대상 국가는 필수입니다.")
        String targetCountry,

        @NotBlank(message = "언어 선택은 필수입니다.")
        @Pattern(
                regexp = "EN|JA|ZH|VI|ES|DE",
                message = "언어는 EN, JA, ZH, VI, ES, DE 중 하나여야 합니다."
        )
        String targetLanguage,

        @NotBlank(message = "관계는 필수입니다.")
        String relationshipType,

        String ageGroup,
        String gender,

        @NotBlank(message = "말투 선택은 필수입니다.")
        @Pattern(
                regexp = "FORMAL|POLITE|FRIENDLY|WARM|PLAYFUL|PLAIN|SINCERE|EMOTIONAL|DIALECT",
                message = "지원하지 않는 말투입니다."
        )
        String speechStyle,

        String characteristic
) {
}
