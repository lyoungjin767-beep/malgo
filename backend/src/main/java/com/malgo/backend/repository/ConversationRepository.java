package com.malgo.backend.repository;

import com.malgo.backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 대화방 저장 및 조회를 담당

public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {
    //최근에 사용한 대화방부터 조회
    List<Conversation> findAllByOrderByUpdatedAtDesc();

    List<Conversation> findByAiPartnerId(Long aiPartnerId);
}