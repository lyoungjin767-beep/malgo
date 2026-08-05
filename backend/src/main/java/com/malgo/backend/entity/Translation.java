package com.malgo.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "translations")
public class Translation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
            String originalText,
            String sourceLanguage,
            String targetLanguage,
            String targetCountry,
            String situation,
            String relationshipType,
            String communicationPurpose,
            String requestedTone
    ) {
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