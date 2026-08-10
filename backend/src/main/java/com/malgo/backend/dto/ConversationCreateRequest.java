package com.malgo.backend.dto;

// 새 대화방을 만들 때 받는 요청 DTO

public record ConversationCreateRequest(
        Long aiPartnerId,
        String situation
) {
}