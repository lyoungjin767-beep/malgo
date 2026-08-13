package com.malgo.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

// 대화방 상세 조회 응답 DTO
// 대화방 기본 정보와 AI 상대 정보, 저장된 메시지 목록을 함께 반환

public record ConversationDetailResponse(
        Long conversationId,
        Long aiPartnerId,
        String aiPartnerName,
        String targetCountry,
        String relationshipType,
        String situation,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ConversationMessageResponse> messages
) {
}