package com.malgo.backend.controller;

import com.malgo.backend.dto.ConversationMessageMemoRequest;
import com.malgo.backend.dto.ConversationMessageMemoResponse;
import com.malgo.backend.entity.ConversationMessageMemo;
import com.malgo.backend.service.ConversationMessageMemoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversation-messages")
public class ConversationMessageMemoController {

    private final ConversationMessageMemoService memoService;

    public ConversationMessageMemoController(
            ConversationMessageMemoService memoService
    ) {
        this.memoService = memoService;
    }

    // 메모 생성 또는 수정
    @PutMapping("/{messageId}/memo")
    public ResponseEntity<ConversationMessageMemoResponse> saveOrUpdateMemo(
            @PathVariable Long messageId,
            @RequestBody ConversationMessageMemoRequest request
    ) {
        ConversationMessageMemo memo =
                memoService.saveOrUpdateMemo(
                        messageId,
                        request.content()
                );

        return ResponseEntity.ok(
                ConversationMessageMemoResponse.from(memo)
        );
    }

    // 메모 조회
    @GetMapping("/{messageId}/memo")
    public ResponseEntity<ConversationMessageMemoResponse> getMemo(
            @PathVariable Long messageId
    ) {
        ConversationMessageMemo memo =
                memoService.getMemo(messageId);

        return ResponseEntity.ok(
                ConversationMessageMemoResponse.from(memo)
        );
    }

    // 메모 삭제
    @DeleteMapping("/{messageId}/memo")
    public ResponseEntity<Void> deleteMemo(
            @PathVariable Long messageId
    ) {
        memoService.deleteMemo(messageId);

        return ResponseEntity.noContent().build();
    }
}