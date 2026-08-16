package com.malgo.backend.dto;

import com.malgo.backend.entity.ConversationMessageMemo;

import java.time.LocalDateTime;

public record ConversationMessageMemoResponse(
        Long id,
        Long conversationMessageId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ConversationMessageMemoResponse from(
            ConversationMessageMemo memo
    ) {
        return new ConversationMessageMemoResponse(
                memo.getId(),
                memo.getConversationMessage().getId(),
                memo.getContent(),
                memo.getCreatedAt(),
                memo.getUpdatedAt()
        );
    }
}