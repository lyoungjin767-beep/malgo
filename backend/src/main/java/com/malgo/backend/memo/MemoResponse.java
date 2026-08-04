package com.malgo.backend.memo;

import java.time.LocalDateTime;

public record MemoResponse(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt
) {

    public static MemoResponse from(Memo memo) {
        return new MemoResponse(
                memo.getId(),
                memo.getTitle(),
                memo.getContent(),
                memo.getCreatedAt()
        );
    }
}
