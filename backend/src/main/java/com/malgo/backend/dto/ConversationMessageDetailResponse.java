package com.malgo.backend.dto;

import java.time.LocalDateTime;

public record ConversationMessageDetailResponse(
        Long id,
        String senderType,
        String content,
        LocalDateTime createdAt,
        ConversationAnalysisResponse analysis,
        ConversationMessageMemoResponse memo
) {
}