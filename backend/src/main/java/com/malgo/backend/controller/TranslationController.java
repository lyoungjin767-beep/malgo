package com.malgo.backend.controller;

import com.malgo.backend.dto.TranslationRequest;
import com.malgo.backend.dto.TranslationResponse;
import com.malgo.backend.service.TranslationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}