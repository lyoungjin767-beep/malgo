package com.example.project.controller;

import com.example.project.dto.TranslationRequest;
import com.example.project.dto.TranslationResponse;
import com.example.project.service.TranslationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}