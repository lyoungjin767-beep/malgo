package com.malgo.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 하나의 대화방 안에서 주고받은 메시지를 저장
/**
 * 예:
 * USER      -> "미국 친구한테 오늘 못 간다고 말하고 싶어"
 * ASSISTANT -> "I can't make it today..."
 */
@Entity
@Table(name = "conversation_messages")
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 대화방의 메시지인지 연결
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    // 메시지를 누가 보냈는지 구분
    // USER / ASSISTANT
    @Column(nullable = false, length = 20)
    private String senderType;

    // 사용자가 입력했거나 AI가 응답한 실제 메시지 내용
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 메시지가 생성된 시간
    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ConversationMessage() {
    }

    public ConversationMessage(
            Conversation conversation,
            String senderType,
            String content
    ) {
        this.conversation = conversation;
        this.senderType = senderType;
        this.content = content;
    }

    // 메시지를 DB에 처음 저장할 때 생성 시간을 기록
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

    public String getSenderType() {
        return senderType;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}