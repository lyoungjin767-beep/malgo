package com.malgo.backend.dto;

import java.time.LocalDateTime;

// 저장된 대화 메시지를 프론트에 반환하는 DTO

public record ConversationMessageResponse(
        Long id,
        String senderType,
        String content,
        LocalDateTime createdAt
) {
}