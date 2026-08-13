package com.malgo.backend.repository;

import com.malgo.backend.entity.Translation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranslationRepository
        extends JpaRepository<Translation, Long> {

    List<Translation> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}