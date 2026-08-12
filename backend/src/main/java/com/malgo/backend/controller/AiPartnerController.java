package com.malgo.backend.controller;

import com.malgo.backend.dto.AiPartnerResponse;
import com.malgo.backend.service.AiPartnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.malgo.backend.dto.AiPartnerCreateRequest;
import com.malgo.backend.dto.AiPartnerUpdateRequest;

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

    // 커스텀 AI 상대 생성
    // POST /api/partners
    @PostMapping
    public ResponseEntity<AiPartnerResponse> createPartner(
            @RequestBody AiPartnerCreateRequest request
    ) {
        return ResponseEntity.ok(
                aiPartnerService.createCustomPartner(request)
        );
    }


    // 커스텀 AI 상대 수정
    // PUT /api/partners/{id}
    @PutMapping("/{id}")
    public ResponseEntity<AiPartnerResponse> updatePartner(
            @PathVariable Long id,
            @RequestBody AiPartnerUpdateRequest request
    ) {
        return ResponseEntity.ok(
                aiPartnerService.updatePartner(id, request)
        );
    }
}