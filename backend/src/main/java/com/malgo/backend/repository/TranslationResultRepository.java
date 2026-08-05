package com.malgo.backend.repository;

import com.malgo.backend.entity.TranslationResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranslationResultRepository
        extends JpaRepository<TranslationResult, Long> {
}