package com.malgo.backend.dto;

// 생성된 대화방 정보를 프론트에 반환하는 DTO

public record ConversationResponse(
        Long id,
        Long aiPartnerId,
        String aiPartnerName,
        String situation,
        String field
) {
}