package com.malgo.backend.repository;

import com.malgo.backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 대화방 저장 및 조회를 담당

public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {

    List<Conversation> findByAiPartnerId(Long aiPartnerId);

    List<Conversation> findByMemberIdOrderByUpdatedAtDesc(Long memberId);
}