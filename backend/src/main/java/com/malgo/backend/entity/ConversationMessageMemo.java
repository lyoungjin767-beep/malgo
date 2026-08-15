package com.malgo.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_message_memos")
public class ConversationMessageMemo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 AI 답변에 작성한 메모인지 연결
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "conversation_message_id",
            nullable = false,
            unique = true
    )
    private ConversationMessage conversationMessage;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ConversationMessageMemo() {
    }

    public ConversationMessageMemo(
            ConversationMessage conversationMessage,
            String content
    ) {
        this.conversationMessage = conversationMessage;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ConversationMessage getConversationMessage() {
        return conversationMessage;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}