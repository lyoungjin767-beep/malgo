package com.malgo.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "culture_warnings")
public class CultureWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "translation_result_id", nullable = false)
    private TranslationResult translationResult;

    @Column(nullable = false, length = 500)
    private String expression;

    @Column(length = 30)
    private String category;

    @Column(nullable = false, length = 20)
    private String riskLevel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String alternativeExpression;

    private Integer startIndex;

    private Integer endIndex;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected CultureWarning() {
    }

    public CultureWarning(
            TranslationResult translationResult,
            String expression,
            String category,
            String riskLevel,
            String reason,
            String alternativeExpression,
            Integer startIndex,
            Integer endIndex
    ) {
        this.translationResult = translationResult;
        this.expression = expression;
        this.category = category;
        this.riskLevel = riskLevel;
        this.reason = reason;
        this.alternativeExpression = alternativeExpression;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }
}