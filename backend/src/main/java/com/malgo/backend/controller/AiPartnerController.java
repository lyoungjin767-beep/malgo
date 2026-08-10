package com.malgo.backend.controller;

import com.malgo.backend.dto.AiPartnerResponse;
import com.malgo.backend.service.AiPartnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


// AI 대화 상대 관련 API

@RestController
@RequestMapping("/api/partners")
public class AiPartnerController {

    private final AiPartnerService aiPartnerService;

    public AiPartnerController(
            AiPartnerService aiPartnerService
    ) {
        this.aiPartnerService = aiPartnerService;
    }

    // AI 대화 상대 목록 조회
    // GET /api/partners
    @GetMapping
    public ResponseEntity<List<AiPartnerResponse>> getPartners() {

        return ResponseEntity.ok(
                aiPartnerService.getPartners()
        );
    }
}