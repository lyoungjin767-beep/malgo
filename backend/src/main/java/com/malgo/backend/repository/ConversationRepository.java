package com.malgo.backend.repository;

import com.malgo.backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

// 대화방 저장 및 조회를 담당

public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {
}