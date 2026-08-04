package com.malgo.backend.ai;

import com.malgo.backend.dto.TranslationRequest;
import com.malgo.backend.dto.TranslationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private final RestClient restClient;
    private final String model;

    public OpenAiClient(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model}") String model
    ) {
        this.model = model;

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public TranslationResponse translate(TranslationRequest request) {
        String prompt = buildPrompt(request);

        Map<String, Object> body = Map.of(
                "model", model,
                "input", prompt
        );

        Map<?, ?> response = restClient.post()
                .uri("/responses")
                .body(body)
                .retrieve()
                .body(Map.class);

        String outputText = extractOutputText(response);

        // OpenAI 응답을 TranslationResponse로 변환 예정
        throw new UnsupportedOperationException(
                "OpenAI 응답 확인 완료: " + outputText
        );
    }

    private String buildPrompt(TranslationRequest request) {
        return """
                너는 문화적 맥락을 고려하는 전문 번역가다.

                다음 정보를 바탕으로 문장을 분석하고 번역하라.

                원문: %s
                원문 언어: %s
                목표 언어: %s
                대상 국가: %s
                상황: %s
                관계: %s
                목적: %s
                요청 말투: %s

                우선 테스트를 위해 번역 결과를 간단한 문장으로 반환하라.
                """
                .formatted(
                        request.originalText(),
                        request.sourceLanguage(),
                        request.targetLanguage(),
                        request.targetCountry(),
                        request.situation(),
                        request.relationshipType(),
                        request.communicationPurpose(),
                        request.requestedTone()
                );
    }

    private String extractOutputText(Map<?, ?> response) {
        if (response == null) {
            throw new IllegalStateException("OpenAI 응답이 비어 있습니다.");
        }

        Object outputObject = response.get("output");

        if (!(outputObject instanceof List<?> outputList)
                || outputList.isEmpty()) {
            throw new IllegalStateException("OpenAI 응답에 output이 없습니다.");
        }

        for (Object outputItem : outputList) {
            if (!(outputItem instanceof Map<?, ?> outputMap)) {
                continue;
            }

            Object contentObject = outputMap.get("content");

            if (!(contentObject instanceof List<?> contentList)) {
                continue;
            }

            for (Object contentItem : contentList) {
                if (!(contentItem instanceof Map<?, ?> contentMap)) {
                    continue;
                }

                Object type = contentMap.get("type");
                Object text = contentMap.get("text");

                if ("output_text".equals(type) && text instanceof String outputText) {
                    return outputText;
                }
            }
        }

        throw new IllegalStateException(
                "OpenAI 응답에서 output_text를 찾지 못했습니다. 전체 응답: " + response
        );
    }
}