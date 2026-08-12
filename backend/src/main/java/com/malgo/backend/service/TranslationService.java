package com.malgo.backend.service;

import com.malgo.backend.ai.OpenAiClient;
import com.malgo.backend.dto.*;
import com.malgo.backend.exception.TranslationNotFoundException;
import org.springframework.stereotype.Service;

import com.malgo.backend.entity.Translation;
import com.malgo.backend.entity.TranslationResult;
import com.malgo.backend.entity.CultureWarning;

import com.malgo.backend.repository.TranslationRepository;
import com.malgo.backend.repository.TranslationResultRepository;
import com.malgo.backend.repository.CultureWarningRepository;

import org.springframework.transaction.annotation.Transactional;

import com.malgo.backend.dto.TranslationMemoRequest;
import com.malgo.backend.dto.TranslationMemoResponse;
import com.malgo.backend.entity.TranslationMemo;
import com.malgo.backend.repository.TranslationMemoRepository;
import com.malgo.backend.dto.MyPageTranslationResponse;
import com.malgo.backend.dto.TranslationStatisticsResponse;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TranslationService {

    private final TranslationRepository translationRepository;
    private final TranslationResultRepository translationResultRepository;
    private final CultureWarningRepository cultureWarningRepository;
    private final OpenAiClient openAiClient;
    private final TranslationMemoRepository translationMemoRepository;

    public TranslationService(
            TranslationRepository translationRepository,
            TranslationResultRepository translationResultRepository,
            CultureWarningRepository cultureWarningRepository,
            OpenAiClient openAiClient,
            TranslationMemoRepository translationMemoRepository
    ) {
        this.translationRepository = translationRepository;
        this.translationResultRepository = translationResultRepository;
        this.cultureWarningRepository = cultureWarningRepository;
        this.openAiClient = openAiClient;
        this.translationMemoRepository = translationMemoRepository;
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
                        new TranslationNotFoundException(translationId)
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

        // 번역 기록에 작성된 메모가 있다면 가져온다.
        // 메모가 없으면 null을 반환한다.
        String memo = translationMemoRepository
                .findByTranslationId(translationId)
                .map(TranslationMemo::getContent)
                .orElse(null);

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
                warnings,

                memo,
                translation.getCreatedAt()
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
                    new TranslationNotFoundException(translationId)
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

    // 번역 기록에 메모를 저장하거나 기존 메모를 수정
    // 이미 메모가 있으면 수정하고, 없으면 새 메모를 생성
    @Transactional
    public TranslationMemoResponse saveOrUpdateMemo(
            Long translationId,
            TranslationMemoRequest request
    ) {

        // 1. 번역 기록 존재 여부 확인
        Translation translation = translationRepository.findById(translationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "번역 기록을 찾을 수 없습니다. id=" + translationId
                        )
                );

        // 2. 기존 메모가 있는지 확인
        TranslationMemo memo = translationMemoRepository
                .findByTranslationId(translationId)
                .orElseGet(() ->
                        new TranslationMemo(
                                translation,
                                request.content()
                        )
                );

        // 기존 메모가 있는 경우 내용 수정
        if (memo.getId() != null) {
            memo.updateContent(request.content());
        }

        // 3. 메모 저장
        TranslationMemo savedMemo =
                translationMemoRepository.save(memo);

        // 4. 응답 DTO 반환
        return new TranslationMemoResponse(
                savedMemo.getId(),
                translationId,
                savedMemo.getContent(),
                savedMemo.getCreatedAt(),
                savedMemo.getUpdatedAt()
        );
    }


    // 특정 번역 기록에 저장된 메모를 조회
    @Transactional(readOnly = true)
    public TranslationMemoResponse getMemo(Long translationId) {

        // 번역 기록 자체가 존재하는지 확인
        if (!translationRepository.existsById(translationId)) {
            throw new IllegalArgumentException(
                    "번역 기록을 찾을 수 없습니다. id=" + translationId
            );
        }

        TranslationMemo memo = translationMemoRepository
                .findByTranslationId(translationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "저장된 메모가 없습니다."
                        )
                );

        return new TranslationMemoResponse(
                memo.getId(),
                translationId,
                memo.getContent(),
                memo.getCreatedAt(),
                memo.getUpdatedAt()
        );
    }

    // 마이페이지에 표시할 최근 번역 기록을 조회한다.
    // 원문, 추천 번역, 번역 날짜, 메모 존재 여부를 함께 반환
    @Transactional(readOnly = true)
    public List<MyPageTranslationResponse> getMyPageTranslations() {

        return translationRepository.findAll()
                .stream()

                // 최신 번역이 위에 나오도록 정렬
                .sorted((a, b) ->
                        b.getCreatedAt().compareTo(a.getCreatedAt())
                )

                .map(translation -> {

                    // 해당 번역의 AI 분석 결과 조회
                    TranslationResult result =
                            translationResultRepository
                                    .findByTranslationId(translation.getId())
                                    .orElse(null);

                    // 결과가 없는 번역 기록은 제외
                    if (result == null) {
                        return null;
                    }

                    // 메모 존재 여부 확인
                    boolean hasMemo =
                            translationMemoRepository
                                    .existsByTranslationId(
                                            translation.getId()
                                    );

                    return new MyPageTranslationResponse(
                            translation.getId(),
                            translation.getOriginalText(),

                            // 디자인의 '추천 번역'
                            result.getCulturalTranslation(),

                            translation.getCreatedAt(),
                            hasMemo
                    );
                })

                .filter(Objects::nonNull)
                .toList();
    }


    // 저장된 번역 기록을 상황별로 집계한다.
    // 예: BUSINESS -> 8, DAILY -> 5, TRAVEL -> 3
    @Transactional(readOnly = true)
    public TranslationStatisticsResponse getTranslationStatistics() {

        List<Translation> translations =
                translationRepository.findAll();

        Map<String, Long> situationCounts =
                translations.stream()
                        .filter(translation ->
                                translation.getSituation() != null
                        )
                        .collect(
                                java.util.stream.Collectors.groupingBy(
                                        Translation::getSituation,
                                        java.util.stream.Collectors.counting()
                                )
                        );

        return new TranslationStatisticsResponse(
                translations.size(),
                situationCounts
        );
    }
}