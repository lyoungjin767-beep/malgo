package com.malgo.backend.repository;

import com.malgo.backend.entity.Translation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranslationRepository
        extends JpaRepository<Translation, Long> {

    // 번역 기록을 최신 생성 순서로 조회
    // Spring Data JPA가 메서드 이름을 분석해 createdAt DESC 조건의 쿼리를 자동 생성

    List<Translation> findAllByOrderByCreatedAtDesc();
}