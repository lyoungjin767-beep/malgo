package com.malgo.backend.controller;

import com.malgo.backend.dto.*;
import com.malgo.backend.service.TranslationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import com.malgo.backend.dto.TranslationStatisticsResponse;

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

    // 특정 회원의 저장된 번역 기록 목록 조회
    // GET /api/translations/member/{memberId}
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<TranslationHistoryResponse>> getTranslationHistory(
            @PathVariable Long memberId
    ) {

        List<TranslationHistoryResponse> history =
                translationService.getTranslationHistory(memberId);

        return ResponseEntity.ok(history);
    }

    // 번역 기록 상세 조회
    // URL 예시 : GET /api/translations/1
    @GetMapping("/member/{memberId}/{id}")
    public ResponseEntity<TranslationDetailResponse> getTranslationDetail(
            @PathVariable Long memberId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                translationService.getTranslationDetail(memberId, id)
        );
    }

    // 번역 기록 삭제
    // URL 예시: DELETE /api/translations/1
    @DeleteMapping("/member/{memberId}/{id}")
    public ResponseEntity<Void> deleteTranslation(
            @PathVariable Long memberId,
            @PathVariable Long id
    ) {
        translationService.deleteTranslation(memberId, id);
        // 삭제 성공 시 본문 없이 204 No Content 반환
        return ResponseEntity.noContent().build();
    }

    // 번역 기록 메모 저장 또는 수정
    // PUT /api/translations/{id}/memo
    @PutMapping("/member/{memberId}/{id}/memo")
    public ResponseEntity<TranslationMemoResponse> saveOrUpdateMemo(
            @PathVariable Long memberId,
            @PathVariable Long id,
            @Valid @RequestBody TranslationMemoRequest request
    ) {
        return ResponseEntity.ok(
                translationService.saveOrUpdateMemo(
                        memberId,
                        id,
                        request
                )
        );
    }

    // 특정 회원의 번역 기록 메모 조회
    // GET /api/translations/member/{memberId}/{id}/memo
    @GetMapping("/member/{memberId}/{id}/memo")
    public ResponseEntity<TranslationMemoResponse> getMemo(
            @PathVariable Long memberId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                translationService.getMemo(memberId, id)
        );
    }

    // 특정 회원의 마이페이지 최근 번역 기록 조회
    // GET /api/translations/member/{memberId}/recent
    @GetMapping("/member/{memberId}/recent")
    public ResponseEntity<List<MyPageTranslationResponse>>
    getRecentTranslations(
            @PathVariable Long memberId
    ) {

        return ResponseEntity.ok(
                translationService.getMyPageTranslations(memberId)
        );
    }

    // 특정 회원의 번역 상황별 사용 통계 조회
    // GET /api/translations/member/{memberId}/statistics
    @GetMapping("/member/{memberId}/statistics")
    public ResponseEntity<TranslationStatisticsResponse>
    getTranslationStatistics(
            @PathVariable Long memberId
    ) {

        return ResponseEntity.ok(
                translationService.getTranslationStatistics(memberId)
        );
    }

    // 특정 회원의 번역 기록 메모 삭제
    // DELETE /api/translations/member/{memberId}/{id}/memo
    @DeleteMapping("/member/{memberId}/{id}/memo")
    public ResponseEntity<Void> deleteMemo(
            @PathVariable Long memberId,
            @PathVariable Long id
    ) {

        translationService.deleteMemo(memberId, id);

        return ResponseEntity.noContent().build();
    }
}