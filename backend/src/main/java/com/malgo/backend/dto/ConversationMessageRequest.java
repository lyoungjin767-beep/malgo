package com.malgo.backend.dto;

// 대화방에 새 메시지를 저장할 때 사용하는 요청 DTO

public record ConversationMessageRequest(
        String content
) {
}