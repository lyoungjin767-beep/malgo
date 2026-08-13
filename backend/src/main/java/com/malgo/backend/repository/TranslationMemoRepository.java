package com.malgo.backend.repository;

import com.malgo.backend.entity.TranslationMemo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 번역 기록 메모 저장/조회를 담당

public interface TranslationMemoRepository
        extends JpaRepository<TranslationMemo, Long> {

    Optional<TranslationMemo> findByTranslationId(Long translationId);

    // 해당 번역 기록에 메모가 존재하는지 확인
    boolean existsByTranslationId(Long translationId);
}