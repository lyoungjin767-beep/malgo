package com.malgo.backend.repository;

import com.malgo.backend.entity.ConversationMessageMemo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationMessageMemoRepository
        extends JpaRepository<ConversationMessageMemo, Long> {

    Optional<ConversationMessageMemo> findByConversationMessageId(
            Long conversationMessageId
    );
}