package com.malgo.backend.entity;

import jakarta.persistence.*;
import com.malgo.backend.member.entity.Member;

import java.time.LocalDateTime;

// 사용자와 AI 상대가 나누는 하나의 대화방을 저장
// 예: 사용자가 kash를 선택하고 대화를 시작하면 Conversation 1개가 생성

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // AI Partner를 선택한 경우 연결
    // 직접 설정으로 대화하는 경우 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_partner_id")
    private AiPartner aiPartner;

    // 어떤 회원의 대화방인지 연결
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 대화의 주제 또는 상황
    // 예: BUSINESS, DAILY, TRAVEL 등
    @Column(length = 30)
    private String situation;

    // 사용자가 선택한 업무 분야
    // 예: IT_DEVELOPMENT, DESIGN, MARKETING, SALES, FINANCE
    @Column(length = 30)
    private String field;

    // 대화가 시작된 시간
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 가장 최근 메시지가 오간 시간
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // AI Partner를 선택하지 않았을 때 사용하는 직접 설정 정보

    @Column(name = "target_language", length = 10, nullable = false)
    private String targetLanguage;

    @Column(length = 10)
    private String targetCountry;

    @Column(length = 30)
    private String relationshipType;

    @Column(length = 30)
    private String ageGroup;

    @Column(length = 30)
    private String speechStyle;

    @Column(columnDefinition = "TEXT")
    private String characteristic;

    protected Conversation() {
    }

    public Conversation(
            Member member,
            AiPartner aiPartner,
            String situation,
            String field,
            String targetLanguage,
            String targetCountry,
            String relationshipType,
            String ageGroup,
            String speechStyle,
            String characteristic
    ) {
        this.member = member;
        this.aiPartner = aiPartner;
        this.situation = situation;
        this.field = field;
        this.targetLanguage = targetLanguage;
        this.targetCountry = targetCountry;
        this.relationshipType = relationshipType;
        this.ageGroup = ageGroup;
        this.speechStyle = speechStyle;
        this.characteristic = characteristic;
    }

    // 대화방을 처음 저장할 때 생성 시간과 수정 시간을 자동으로 설정
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    // 대화방 정보가 수정될 때 최근 수정 시간을 갱신
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public AiPartner getAiPartner() {
        return aiPartner;
    }

    public String getSituation() {
        return situation;
    }

    public String getField(){return field;}

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Member getMember() {
        return member;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public String getTargetCountry() {
        return targetCountry;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public String getAgeGroup() {
        return ageGroup;
    }

    public String getSpeechStyle() {
        return speechStyle;
    }

    public String getCharacteristic() {
        return characteristic;
    }

    // 새로운 메시지가 생성됐을 때
    public void updateLastActivity() {
        this.updatedAt = LocalDateTime.now();
    }
}