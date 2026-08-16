package com.malgo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// 새 대화방을 만들 때 받는 요청 DTO

public record ConversationCreateRequest(
        @NotNull(message = "회원 ID는 필수입니다.")
        Long memberId,

        // AI Partner를 선택하지 않는 경우 null 가능
        Long aiPartnerId,

        @NotBlank(message = "대화 상황은 필수입니다.")
        String situation,

        @NotBlank(message = "분야 선택은 필수입니다.")
        @Pattern(
                regexp = "IT_DEVELOPMENT|DESIGN|MARKETING|SALES|FINANCE",
                message = "분야는 IT_DEVELOPMENT, DESIGN, MARKETING, SALES, FINANCE 중 하나여야 합니다."
        )
        String field,

        @NotBlank(message = "언어 선택은 필수입니다.")
        @Pattern(
                regexp = "EN|JA|ZH|VI|ES|DE",
                message = "언어는 EN, JA, ZH, VI, ES, DE 중 하나여야 합니다."
        )
        String targetLanguage,

        // AI Partner 미선택 시 직접 설정
        String targetCountry,

        String relationshipType,
        String ageGroup,

        @Pattern(
                regexp = "FORMAL|POLITE|FRIENDLY|WARM|PLAYFUL|PLAIN|SINCERE|EMOTIONAL|DIALECT",
                message = "지원하지 않는 말투입니다."
        )
        String speechStyle,

        String characteristic
) {
}
