package com.malgo.backend.entity;

import com.malgo.backend.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_partners")
public class AiPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 커스텀 AI를 만든 회원. 기본 제공 AI는 member가 null이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 10)
    private String targetCountry;

    // AI가 사용할 언어
    // EN, JA, ZH, VI, ES, DE
    @Column(nullable = false, length = 10)
    private String targetLanguage;

    @Column(nullable = false, length = 30)
    private String relationshipType;

    @Column(length = 30)
    private String ageGroup;

    @Column(length = 20)
    private String gender;

    @Column(length = 30)
    private String speechStyle;

    @Column(columnDefinition = "TEXT")
    private String characteristic;

    @Column(nullable = false)
    private boolean custom;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected AiPartner() {
    }

    public AiPartner(
            String name,
            String targetCountry,
            String targetLanguage,
            String relationshipType,
            String ageGroup,
            String gender,
            String speechStyle,
            String characteristic,
            boolean custom
    ) {
        this.name = name;
        this.targetCountry = targetCountry;
        this.targetLanguage = targetLanguage;
        this.relationshipType = relationshipType;
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.speechStyle = speechStyle;
        this.characteristic = characteristic;
        this.custom = custom;
    }

    public AiPartner(
            Member member,
            String name,
            String targetCountry,
            String targetLanguage,
            String relationshipType,
            String ageGroup,
            String gender,
            String speechStyle,
            String characteristic,
            boolean custom
    ) {
        this.member = member;
        this.name = name;
        this.targetCountry = targetCountry;
        this.targetLanguage = targetLanguage;
        this.relationshipType = relationshipType;
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.speechStyle = speechStyle;
        this.characteristic = characteristic;
        this.custom = custom;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public String getName() {
        return name;
    }

    public String getTargetCountry() {
        return targetCountry;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public String getAgeGroup() {
        return ageGroup;
    }

    public String getGender() {
        return gender;
    }

    public String getSpeechStyle() {
        return speechStyle;
    }

    public String getCharacteristic() {
        return characteristic;
    }

    public boolean isCustom() {
        return custom;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // 커스텀 AI 설정 수정
    public void update(
            String name,
            String targetCountry,
            String targetLanguage,
            String relationshipType,
            String ageGroup,
            String gender,
            String speechStyle,
            String characteristic
    ) {
        this.name = name;
        this.targetCountry = targetCountry;
        this.targetLanguage = targetLanguage;
        this.relationshipType = relationshipType;
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.speechStyle = speechStyle;
        this.characteristic = characteristic;
    }
}
