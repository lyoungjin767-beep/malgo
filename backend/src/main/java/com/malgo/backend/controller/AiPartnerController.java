package com.malgo.backend.controller;

import com.malgo.backend.dto.AiPartnerResponse;
import com.malgo.backend.service.AiPartnerService;
import jakarta.validation.Valid;
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

    // 기본 AI + 해당 회원이 만든 커스텀 AI 목록 조회
// GET /api/partners/member/{memberId}
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<AiPartnerResponse>> getPartners(
            @PathVariable Long memberId
    ) {

        return ResponseEntity.ok(
                aiPartnerService.getPartners(memberId)
        );
    }

    // 커스텀 AI 상대 생성
    // POST /api/partners
    @PostMapping("/member/{memberId}")
    public ResponseEntity<AiPartnerResponse> createCustomPartner(
            @PathVariable Long memberId,
            @Valid @RequestBody AiPartnerCreateRequest request
    ) {
        return ResponseEntity.ok(
                aiPartnerService.createCustomPartner(
                        memberId,
                        request
                )
        );
    }


    // 커스텀 AI 상대 수정
    // PUT /api/partners/{id}
    @PutMapping("/member/{memberId}/{id}")
    public ResponseEntity<AiPartnerResponse> updatePartner(
            @PathVariable Long memberId,
            @PathVariable Long id,
            @Valid @RequestBody AiPartnerUpdateRequest request
    ) {
        return ResponseEntity.ok(
                aiPartnerService.updatePartner(memberId, id, request)
        );
    }

    // 커스텀 AI 상대 삭제
    // DELETE /api/partners/{id}
    @DeleteMapping("/member/{memberId}/{id}")
    public ResponseEntity<Void> deletePartner(
            @PathVariable Long memberId,
            @PathVariable Long id
    ) {

        aiPartnerService.deletePartner(memberId, id);

        return ResponseEntity.noContent().build();
    }

    // AI 상대 상세 조회
    // GET /api/partners/{id}
    @GetMapping("/member/{memberId}/{id}")
    public ResponseEntity<AiPartnerResponse> getPartner(
            @PathVariable Long memberId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                aiPartnerService.getPartner(memberId, id)
        );
    }
}