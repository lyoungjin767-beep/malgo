package com.malgo.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 대화 내용 요약 결과를 저장하는 엔티티
// 하나의 Conversation에 대해 생성된 AI 요약 결과를 저장

@Entity
@Table(name = "conversation_summaries")
public class ConversationSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 대화방의 요약인지 연결
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    // OpenAI가 생성한 실제 요약 내용
    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    // 요약이 생성된 시간
    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ConversationSummary() {
    }

    public ConversationSummary(
            Conversation conversation,
            String summary
    ) {
        this.conversation = conversation;
        this.summary = summary;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public String getSummary() {
        return summary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}