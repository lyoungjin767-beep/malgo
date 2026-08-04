package com.malgo.backend.memo;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

public record MemoCreateRequest(
        @Size(max = 100)
        String title,

        @Size(max = 1000)
        String content,

        @Size(max = 1000)
        String memo
) {

    @AssertTrue(message = "content 또는 memo 값은 필수입니다.")
    public boolean hasMemoText() {
        return StringUtils.hasText(content) || StringUtils.hasText(memo);
    }

    public String normalizedContent() {
        String value = StringUtils.hasText(content) ? content : memo;
        return value.trim();
    }

    public String normalizedTitle() {
        return StringUtils.hasText(title) ? title.trim() : null;
    }
}
