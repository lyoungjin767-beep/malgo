package com.malgo.backend.service;

import com.malgo.backend.ai.OpenAiClient;
import com.malgo.backend.dto.TranslationRequest;
import com.malgo.backend.dto.TranslationResponse;
import org.springframework.stereotype.Service;

import com.malgo.backend.entity.Translation;
import com.malgo.backend.entity.TranslationResult;
import com.malgo.backend.entity.CultureWarning;

import com.malgo.backend.repository.TranslationRepository;
import com.malgo.backend.repository.TranslationResultRepository;
import com.malgo.backend.repository.CultureWarningRepository;

import org.springframework.transaction.annotation.Transactional;

import com.malgo.backend.dto.TranslationHistoryResponse;
import java.util.List;

@Service
public class TranslationService {

    private final TranslationRepository translationRepository;
    private final TranslationResultRepository translationResultRepository;
    private final CultureWarningRepository cultureWarningRepository;
    private final OpenAiClient openAiClient;

    public TranslationService(
            TranslationRepository translationRepository,
            TranslationResultRepository translationResultRepository,
            CultureWarningRepository cultureWarningRepository,
            OpenAiClient openAiClient
    ) {
        this.translationRepository = translationRepository;
        this.translationResultRepository = translationResultRepository;
        this.cultureWarningRepository = cultureWarningRepository;
        this.openAiClient = openAiClient;
    }


    // 번역 요청을 처리하고 AI 분석 결과를 DB에 저장
    /**처리 순서
     * 1. 사용자가 입력한 번역 요청 저장
     * 2. OpenAI API 호출
     * 3. 번역 결과 저장
     * 4. 위험 표현 목록 저장
     * 5. 최종 번역 결과 반환
     */
    @Transactional
    public TranslationResponse analyze(TranslationRequest request) {

        // 1. 사용자가 입력한 원문과 상황 정보를 translations 테이블에 저장
        Translation translation = new Translation(
                request.originalText(),
                request.sourceLanguage(),
                request.targetLanguage(),
                request.targetCountry(),
                request.situation(),
                request.relationshipType(),
                request.communicationPurpose(),
                request.requestedTone()
        );

        Translation savedTranslation =
                translationRepository.save(translation);

        // 2. OpenAI API를 호출해 실제 번역 분석 결과를 받음
        TranslationResponse response =
                openAiClient.translate(request);

        // 3. 번역 결과와 말투 점수를 translation_results 테이블에 저장
        TranslationResult translationResult = new TranslationResult(
                savedTranslation,
                response.literalTranslation(),
                response.naturalTranslation(),
                response.culturalTranslation(),
                response.culturalExplanation(),
                response.overallRiskLevel(),
                response.toneScores().friendliness(),
                response.toneScores().politeness(),
                response.toneScores().directness(),
                response.toneScores().aggression(),
                response.toneScores().burden(),
                response.toneScores().professionalism(),
                response.toneScores().naturalness()
        );

        TranslationResult savedResult =
                translationResultRepository.save(translationResult);

        // 4. 위험 표현이 있다면 culture_warnings 테이블에 각각 저장
        if (response.warnings() != null) {
            response.warnings().forEach(warningResponse -> {

                CultureWarning warning = new CultureWarning(
                        savedResult,
                        warningResponse.expression(),
                        warningResponse.category(),
                        warningResponse.riskLevel(),
                        warningResponse.reason(),
                        warningResponse.alternativeExpression(),
                        warningResponse.startIndex(),
                        warningResponse.endIndex()
                );

                cultureWarningRepository.save(warning);
            });
        }

        // 5. DB 저장 후 OpenAI 분석 결과를 프론트에 그대로 반환
        return response;
    }

    // 저장된 번역 기록을 최신순으로 조회
    // Entity를 그대로 반환하지 않고 DTO로 변환
    // 프론트에 필요한 정보만 전달

    @Transactional(readOnly = true)
    public List<TranslationHistoryResponse> getTranslationHistory() {

        return translationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(translation -> new TranslationHistoryResponse(
                        translation.getId(),
                        translation.getOriginalText(),
                        translation.getSourceLanguage(),
                        translation.getTargetLanguage(),
                        translation.getTargetCountry(),
                        translation.getSituation(),
                        translation.getCreatedAt()
                ))
                .toList();
    }
}