package com.malgo.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "translation_results")
public class TranslationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "translation_id",
            nullable = false,
            unique = true
    )
    private Translation translation;

    @Column(columnDefinition = "TEXT")
    private String literalTranslation;

    @Column(columnDefinition = "TEXT")
    private String naturalTranslation;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String culturalTranslation;

    @Column(columnDefinition = "TEXT")
    private String culturalExplanation;

    @Column(length = 20)
    private String overallRiskLevel;

    private Integer friendlinessScore;
    private Integer politenessScore;
    private Integer directnessScore;
    private Integer aggressionScore;
    private Integer burdenScore;
    private Integer professionalismScore;
    private Integer naturalnessScore;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected TranslationResult() {
    }

    public TranslationResult(
            Translation translation,
            String literalTranslation,
            String naturalTranslation,
            String culturalTranslation,
            String culturalExplanation,
            String overallRiskLevel,
            Integer friendlinessScore,
            Integer politenessScore,
            Integer directnessScore,
            Integer aggressionScore,
            Integer burdenScore,
            Integer professionalismScore,
            Integer naturalnessScore
    ) {
        this.translation = translation;
        this.literalTranslation = literalTranslation;
        this.naturalTranslation = naturalTranslation;
        this.culturalTranslation = culturalTranslation;
        this.culturalExplanation = culturalExplanation;
        this.overallRiskLevel = overallRiskLevel;
        this.friendlinessScore = friendlinessScore;
        this.politenessScore = politenessScore;
        this.directnessScore = directnessScore;
        this.aggressionScore = aggressionScore;
        this.burdenScore = burdenScore;
        this.professionalismScore = professionalismScore;
        this.naturalnessScore = naturalnessScore;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Translation getTranslation() {
        return translation;
    }
}