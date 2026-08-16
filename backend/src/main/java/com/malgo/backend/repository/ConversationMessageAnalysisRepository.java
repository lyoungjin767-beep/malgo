package com.malgo.backend.repository;

import com.malgo.backend.entity.ConversationMessageAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationMessageAnalysisRepository
        extends JpaRepository<ConversationMessageAnalysis, Long> {

    Optional<ConversationMessageAnalysis> findByConversationMessageId(
            Long conversationMessageId
    );
}