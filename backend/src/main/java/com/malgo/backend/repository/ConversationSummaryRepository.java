package com.malgo.backend.repository;

import com.malgo.backend.entity.ConversationSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 대화 요약 결과 저장 및 조회를 담당

public interface ConversationSummaryRepository
        extends JpaRepository<ConversationSummary, Long> {

    // 특정 대화방의 요약 기록을 최신순으로 조회
    List<ConversationSummary>
    findByConversationIdOrderByCreatedAtDesc(Long conversationId);
}