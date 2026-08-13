package com.malgo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 새 대화방을 만들 때 받는 요청 DTO

public record ConversationCreateRequest(
        @NotNull(message = "회원 ID는 필수입니다.")
        Long memberId,

        @NotNull(message = "AI 상대 ID는 필수입니다.")
        Long aiPartnerId,

        @NotBlank(message = "대화 상황은 필수입니다.")
        String situation
) {
}