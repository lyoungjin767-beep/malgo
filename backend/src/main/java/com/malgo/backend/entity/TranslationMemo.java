package com.malgo.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 번역 기록에 사용자가 작성한 메모를 저장
// 번역 기록 1건당 메모 1개를 갖도록 한다.

@Entity
@Table(name = "translation_memos")
public class TranslationMemo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 번역 기록에 작성한 메모인지 연결
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "translation_id",
            nullable = false,
            unique = true
    )
    private Translation translation;

    // 사용자가 작성한 메모 내용
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected TranslationMemo() {
    }

    public TranslationMemo(
            Translation translation,
            String content
    ) {
        this.translation = translation;
        this.content = content;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 기존 메모를 수정할 때 사용
    public void updateContent(String content) {
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public Translation getTranslation() {
        return translation;
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