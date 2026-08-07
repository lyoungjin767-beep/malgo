package com.malgo.backend.service;

import com.malgo.backend.ai.OpenAiClient;
import com.malgo.backend.dto.*;
import org.springframework.stereotype.Service;

import com.malgo.backend.entity.Translation;
import com.malgo.backend.entity.TranslationResult;
import com.malgo.backend.entity.CultureWarning;

import com.malgo.backend.repository.TranslationRepository;
import com.malgo.backend.repository.TranslationResultRepository;
import com.malgo.backend.repository.CultureWarningRepository;

import org.springframework.transaction.annotation.Transactional;

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

    // 번역 기록 1건의 상세 정보를 조회
    // 처리 순서
    /**
     * 1. 번역 요청 조회
     * 2. 연결된 번역 결과 조회
     * 3. 번역 결과에 연결된 문화적 경고 조회
     * 4. 상세 응답 DTO로 변환해 반환
     */

    @Transactional(readOnly = true)
    public TranslationDetailResponse getTranslationDetail(Long translationId) {

        // 1. URL로 전달받은 ID에 해당하는 번역 요청을 찾는다.
        Translation translation = translationRepository.findById(translationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "번역 기록을 찾을 수 없습니다. id=" + translationId
                        )
                );

        // 2. 번역 요청 ID와 연결된 AI 번역 결과를 찾는다.
        TranslationResult result = translationResultRepository
                .findByTranslationId(translationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "번역 결과를 찾을 수 없습니다. translationId=" + translationId
                        )
                );

        // 3. 말투 점수 컬럼을 ToneScores DTO로 변환한다.
        ToneScores toneScores = new ToneScores(
                result.getFriendlinessScore(),
                result.getPolitenessScore(),
                result.getDirectnessScore(),
                result.getAggressionScore(),
                result.getBurdenScore(),
                result.getProfessionalismScore(),
                result.getNaturalnessScore()
        );

        // 4. DB에 저장된 문화적 경고들을 응답 DTO 목록으로 변환한다.
        List<CultureWarningResponse> warnings =
                cultureWarningRepository.findByTranslationResultId(result.getId())
                        .stream()
                        .map(warning -> new CultureWarningResponse(
                                warning.getExpression(),
                                warning.getCategory(),
                                warning.getRiskLevel(),
                                warning.getReason(),
                                warning.getAlternativeExpression(),
                                warning.getStartIndex(),
                                warning.getEndIndex()
                        ))
                        .toList();

        // 5. 원문, 번역 결과, 점수, 경고를 하나의 상세 응답으로 반환한다.
        return new TranslationDetailResponse(
                translation.getId(),
                translation.getOriginalText(),
                result.getLiteralTranslation(),
                result.getNaturalTranslation(),
                result.getCulturalTranslation(),
                result.getCulturalExplanation(),
                result.getOverallRiskLevel(),
                toneScores,
                warnings
        );
    }

    // 번역 기록 1건을 삭제
    // 삭제 순서
    /** 1. 번역 요청이 존재하는지 확인
     * 2. 연결된 번역 결과 조회
     * 3. 결과에 연결된 문화적 경고 삭제
     * 4. 번역 결과 삭제
     * 5. 번역 요청 삭제
     */
    // 외래키 관계가 있기 때문에 자식 데이터를 먼저 삭제

    @Transactional
    public void deleteTranslation(Long translationId) {

        // 1. 삭제할 번역 요청이 실제로 존재하는지 확인
        Translation translation = translationRepository.findById(translationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "삭제할 번역 기록을 찾을 수 없습니다. id=" + translationId
                        )
                );

        // 2. 해당 번역 요청에 연결된 AI 결과가 있는지 확인
        translationResultRepository.findByTranslationId(translationId)
                .ifPresent(result -> {

                    // 3. 번역 결과에 연결된 문화적 경고를 먼저 삭제
                    List<CultureWarning> warnings =
                            cultureWarningRepository.findByTranslationResultId(result.getId());

                    cultureWarningRepository.deleteAll(warnings);

                    // 4. 문화적 경고 삭제 후 번역 결과 삭제
                    translationResultRepository.delete(result);
                });

        // 5. 마지막으로 번역 요청 삭제
        translationRepository.delete(translation);
    }
}