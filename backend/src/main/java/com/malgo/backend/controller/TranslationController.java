package com.malgo.backend.controller;

import com.malgo.backend.dto.TranslationDetailResponse;
import com.malgo.backend.dto.TranslationRequest;
import com.malgo.backend.dto.TranslationResponse;
import com.malgo.backend.service.TranslationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.DeleteMapping;

import com.malgo.backend.dto.TranslationHistoryResponse;
import java.util.List;

@RestController
@RequestMapping("/api/translations")
public class TranslationController {

    private final TranslationService translationService;

    public TranslationController(TranslationService translationService) {
        this.translationService = translationService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<TranslationResponse> analyze(
            @Valid @RequestBody TranslationRequest request
    ) {
        TranslationResponse response = translationService.analyze(request);
        return ResponseEntity.ok(response);
    }

    // 저장된 번역 기록 목록을 조회
    // GET /api/translations -> RequestMapping이 있기 때문에

    @GetMapping
    public ResponseEntity<List<TranslationHistoryResponse>> getTranslationHistory() {

        List<TranslationHistoryResponse> history =
                translationService.getTranslationHistory();

        return ResponseEntity.ok(history);
    }

    // 번역 기록 상세 조회
    // URL 예시 : GET /api/translations/1

    @GetMapping("/{id}")
    public ResponseEntity<TranslationDetailResponse> getTranslationDetail(
            @PathVariable Long id
    ) {

        TranslationDetailResponse detail =
                translationService.getTranslationDetail(id);

        return ResponseEntity.ok(detail);
    }

    // 번역 기록 삭제
    // URL 예시: DELETE /api/translations/1

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTranslation(
            @PathVariable Long id
    ) {

        translationService.deleteTranslation(id);

        // 삭제 성공 시 본문 없이 204 No Content 반환
        return ResponseEntity.noContent().build();
    }
}