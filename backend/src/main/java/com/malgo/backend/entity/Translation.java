package com.malgo.backend.entity;

import jakarta.persistence.*;
import com.malgo.backend.member.entity.Member;
import java.time.LocalDateTime;

@Entity
@Table(name = "translations")
public class Translation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 회원의 번역 기록인지 연결
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String originalText;

    @Column(nullable = false, length = 10)
    private String sourceLanguage;

    @Column(nullable = false, length = 10)
    private String targetLanguage;

    @Column(nullable = false, length = 10)
    private String targetCountry;

    @Column(nullable = false, length = 30)
    private String situation;

    @Column(length = 30)
    private String relationshipType;

    @Column(length = 30)
    private String communicationPurpose;

    @Column(length = 30)
    private String requestedTone;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Translation() {
    }

    public Translation(
            Member member,
            String originalText,
            String sourceLanguage,
            String targetLanguage,
            String targetCountry,
            String situation,
            String relationshipType,
            String communicationPurpose,
            String requestedTone
    ) {
        this.member = member;
        this.originalText = originalText;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.targetCountry = targetCountry;
        this.situation = situation;
        this.relationshipType = relationshipType;
        this.communicationPurpose = communicationPurpose;
        this.requestedTone = requestedTone;
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    // 번역 기록 DTO를 만들 때 필요한 값들을 조회

    public String getOriginalText() {
        return originalText;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public String getTargetCountry() {
        return targetCountry;
    }

    public String getSituation() {
        return situation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}