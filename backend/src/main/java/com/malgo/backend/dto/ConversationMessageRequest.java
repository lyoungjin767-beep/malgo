package com.malgo.backend.dto;

// 대화방에 새 메시지를 저장할 때 사용하는 요청 DTO

import jakarta.validation.constraints.NotBlank;

public record ConversationMessageRequest(

        @NotBlank(message = "메시지 내용은 필수입니다.")
        String content

) {
}