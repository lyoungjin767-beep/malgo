package com.malgo.backend.entity;

import jakarta.persistence.*;
import com.malgo.backend.member.entity.Member;
import java.time.LocalDateTime;

//사용자가 대화할 AI 상대 정보를 저장하는 엔티티
// 향후 구독 사용자가 AI 메이커에서 만든 커스텀 상대도 이 테이블에 저장

@Entity
@Table(name = "ai_partners")
public class AiPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 커스텀 AI를 만든 회원
    // 기본 제공 AI는 member = null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 화면에 표시할 AI 상대 이름
    @Column(nullable = false, length = 50)
    private String name;

    // 상대방의 국가 코드
    // 예: US, JP, VN
    @Column(nullable = false, length = 10)
    private String targetCountry;

    // 사용자와 상대방의 관계
    // 예: CLIENT, FRIEND, BOSS
    @Column(nullable = false, length = 30)
    private String relationshipType;

    // 상대방의 연령대
    // 예: CHILD, TEENAGER, COLLEGE_STUDENT, WORKER, SENIOR
    @Column(length = 30)
    private String ageGroup;

    // 성별
    // 예: FEMALE, MALE
    @Column(length = 20)
    private String gender;

    // 말투 또는 대화 스타일
    // 예: CASUAL, POLITE, FRIENDLY
    @Column(length = 30)
    private String speechStyle;

    // 사용자가 입력한 상대방의 특징
    // 긴 설명이 들어갈 수 있으므로 TEXT 사용
    @Column(columnDefinition = "TEXT")
    private String characteristic;

    // 구독 AI 메이커로 생성한 상대인지 여부
    @Column(nullable = false)
    private boolean custom;

    // AI 상대 생성 시간
    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected AiPartner() {
    }

    public AiPartner(
            String name,
            String targetCountry,
            String relationshipType,
            String ageGroup,
            String gender,
            String speechStyle,
            String characteristic,
            boolean custom
    ) {
        this.name = name;
        this.targetCountry = targetCountry;
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
        this.relationshipType = relationshipType;
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.speechStyle = speechStyle;
        this.characteristic = characteristic;
        this.custom = custom;
    }

    // DB에 처음 저장되기 직전에 생성 시간을 자동으로 기록
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Member getMember(){return member;}

    public String getName() {
        return name;
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

    // AI 메이커에서 설정한 정보로 기존 AI 상대 정보를 수정
    public void update(
            String name,
            String targetCountry,
            String relationshipType,
            String ageGroup,
            String gender,
            String speechStyle,
            String characteristic
    ) {
        this.name = name;
        this.targetCountry = targetCountry;
        this.relationshipType = relationshipType;
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.speechStyle = speechStyle;
        this.characteristic = characteristic;
    }
}