package com.malgo.backend.repository;

import com.malgo.backend.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 대화 메시지 저장 및 조회를 담당
public interface ConversationMessageRepository
        extends JpaRepository<ConversationMessage, Long> {

    // 특정 대화방의 메시지를 생성 순서대로 조회
    List<ConversationMessage>
    findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}