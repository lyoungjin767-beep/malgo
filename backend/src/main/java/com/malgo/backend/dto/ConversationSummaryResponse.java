package com.malgo.backend.dto;

// 대화 내용 요약 결과를 프론트에 전달하는 DTO

import java.time.LocalDateTime;

public record ConversationSummaryResponse(
        Long id,
        Long conversationId,
        String summary,
        LocalDateTime createdAt
) {
}