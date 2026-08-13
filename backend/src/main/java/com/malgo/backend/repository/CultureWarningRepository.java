package com.malgo.backend.repository;

import com.malgo.backend.entity.CultureWarning;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CultureWarningRepository
        extends JpaRepository<CultureWarning, Long> {
            // 번역 결과 ID에 해당하는 문화적 경고 목록을 조회
            List<CultureWarning> findByTranslationResultId(Long translationResultId);
}