package com.malgo.backend.controller;

import com.malgo.backend.dto.*;
import com.malgo.backend.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.malgo.backend.dto.ConversationSummaryResponse;

import java.util.List;

// 대화방과 메시지 관련 API

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(
            ConversationService conversationService
    ) {
        this.conversationService = conversationService;
    }

    // 새 대화방 생성
    // POST /api/conversations
    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @RequestBody ConversationCreateRequest request
    ) {
        return ResponseEntity.ok(
                conversationService.createConversation(request)
        );
    }

    // 사용자 메시지를 전송하고 AI 응답을 자동 생성
    // POST /api/conversations/{id}/messages

    @PostMapping("/{id}/messages")
    public ResponseEntity<ConversationChatResponse> sendMessage(
            @PathVariable Long id,
            @RequestBody ConversationMessageRequest request
    ) {
        return ResponseEntity.ok(
                conversationService.sendMessage(id, request)
        );
    }

    // 대화방 메시지 조회
    // GET /api/conversations/{id}/messages

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<ConversationMessageResponse>> getMessages(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                conversationService.getMessages(id)
        );
    }

    // 대화 내용 요약
    // POST /api/conversations/{id}/summary
    @PostMapping("/{id}/summary")
    public ResponseEntity<ConversationSummaryResponse> summarizeConversation(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                conversationService.summarizeConversation(id)
        );
    }
}