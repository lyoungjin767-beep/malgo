package com.malgo.backend.dto;

// 대화방 목록에서 보여줄 간단한 정보

import java.time.LocalDateTime;

public record ConversationListResponse(
        Long conversationId,
        Long aiPartnerId,
        String aiPartnerName,
        String targetCountry,
        String relationshipType,
        String situation,
        String lastMessage,
        LocalDateTime updatedAt
) {
}