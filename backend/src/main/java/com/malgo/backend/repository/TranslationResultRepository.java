package com.malgo.backend.repository;

import com.malgo.backend.entity.TranslationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TranslationResultRepository
        extends JpaRepository<TranslationResult, Long> {
            // 번역 요청 ID로 번역 결과를 조회
            Optional<TranslationResult> findByTranslationId(Long translationId);
}