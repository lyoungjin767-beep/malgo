package com.malgo.backend.controller;

import com.malgo.backend.dto.*;
import com.malgo.backend.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.malgo.backend.dto.ConversationSummaryResponse;
import com.malgo.backend.dto.ConversationListResponse;
import com.malgo.backend.dto.ConversationStatisticsResponse;
import com.malgo.backend.dto.ConversationDetailResponse;

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
            @Valid @RequestBody ConversationCreateRequest request
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
            @Valid @RequestBody ConversationMessageRequest request
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

    // 저장된 대화 요약 목록 조회
    // GET /api/conversations/{id}/summaries
    @GetMapping("/{id}/summaries")
    public ResponseEntity<List<ConversationSummaryResponse>> getConversationSummaries(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                conversationService.getConversationSummaries(id)
        );
    }

    // 대화방 목록 조회
    // GET /api/conversations
    @GetMapping
    public ResponseEntity<List<ConversationListResponse>> getConversations() {

        return ResponseEntity.ok(
                conversationService.getConversations()
        );
    }

    // 대화방 삭제
    // DELETE /api/conversations/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable Long id
    ) {
        conversationService.deleteConversation(id);

        return ResponseEntity.noContent().build();
    }

    // 마이페이지 분야별 대화 통계 조회
    // GET /api/conversations/statistics
    @GetMapping("/statistics")
    public ResponseEntity<ConversationStatisticsResponse>
    getConversationStatistics() {

        return ResponseEntity.ok(
                conversationService.getConversationStatistics()
        );
    }

    // 대화방 상세 조회
    // GET /api/conversations/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ConversationDetailResponse> getConversationDetail(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                conversationService.getConversationDetail(id)
        );
    }

    // 가장 최근 대화 요약 조회
    // GET /api/conversations/{id}/summary/latest
    @GetMapping("/{id}/summary/latest")
    public ResponseEntity<ConversationSummaryResponse> getLatestConversationSummary(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                conversationService.getLatestConversationSummary(id)
        );
    }

    // 특정 회원의 대화방 목록 조회
    // GET /api/conversations/member/{memberId}
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<ConversationListResponse>> getConversationsByMember(
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(
                conversationService.getConversationsByMember(memberId)
        );
    }
}