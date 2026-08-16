package com.malgo.backend.dto;

// 사용자 메시지와 자동 생성된 AI 응답을 함께 반환

public record ConversationChatResponse(
        ConversationMessageResponse userMessage,
        ConversationMessageResponse assistantMessage,
        ConversationAnalysisResponse analysis
) {
}