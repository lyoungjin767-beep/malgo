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
    // POST /api/conversations/member/{memberId}/{id}/messages

    @PostMapping("/member/{memberId}/{id}/messages")
    public ResponseEntity<ConversationChatResponse> sendMessage(
            @PathVariable Long memberId,
            @PathVariable Long id,
            @Valid @RequestBody ConversationMessageRequest request
    ) {
        return ResponseEntity.ok(
                conversationService.sendMessage(memberId, id, request)
        );
    }

    // 대화방 메시지 조회
    // GET /api/conversations/member/{memberId}/{id}/messages

    @GetMapping("/member/{memberId}/{id}/messages")
    public ResponseEntity<List<ConversationMessageDetailResponse>> getMessages(
            @PathVariable Long memberId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                conversationService.getMessageDetails(memberId, id)
        );
    }

    // 대화 내용 요약
    // POST /api/conversations/member/{memberId}/{id}/summary
    @PostMapping("/member/{memberId}/{id}/summary")
    public ResponseEntity<ConversationSummaryResponse> summarizeConversation(
            @PathVariable Long memberId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                conversationService.summarizeConversation(memberId, id)
        );
    }

    // 저장된 대화 요약 목록 조회
    // GET /api/conversations/member/{memberId}/{id}/summaries
    @GetMapping("/member/{memberId}/{id}/summaries")
    public ResponseEntity<List<ConversationSummaryResponse>> getConversationSummaries(
            @PathVariable Long memberId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                conversationService.getConversationSummaries(memberId, id)
        );
    }

    // 대화방 삭제
    // DELETE /api/conversations/member/{memberId}/{id}
    @DeleteMapping("/member/{memberId}/{id}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable Long memberId,
            @PathVariable Long id
    ) {
        conversationService.deleteConversation(memberId, id);

        return ResponseEntity.noContent().build();
    }

    // 마이페이지 분야별 대화 통계 조회
    // GET /api/conversations/member/{memberId}/statistics
    @GetMapping("/member/{memberId}/statistics")
    public ResponseEntity<ConversationStatisticsResponse>
    getConversationStatistics(@PathVariable Long memberId) {

        return ResponseEntity.ok(
                conversationService.getConversationStatistics(memberId)
        );
    }

    // 대화방 상세 조회
    // GET /api/conversations/member/{memberId}/{id}
    @GetMapping("/member/{memberId}/{id}")
    public ResponseEntity<ConversationDetailResponse> getConversationDetail(
            @PathVariable Long memberId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                conversationService.getConversationDetail(memberId, id)
        );
    }

    // 가장 최근 대화 요약 조회
    // GET /api/conversations/member/{memberId}/{id}/summary/latest
    @GetMapping("/member/{memberId}/{id}/summary/latest")
    public ResponseEntity<ConversationSummaryResponse> getLatestConversationSummary(
            @PathVariable Long memberId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                conversationService.getLatestConversationSummary(memberId, id)
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