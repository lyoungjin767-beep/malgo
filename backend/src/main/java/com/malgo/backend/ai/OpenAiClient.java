package com.malgo.backend.ai;

import tools.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public OpenAiClient(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model}") String model,
            ObjectMapper objectMapper
    ) {
        this.model = model;
        this.objectMapper = objectMapper;

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public TranslationResponse translate(TranslationRequest request) {
        String prompt = buildPrompt(request);

        Map<String, Object> body = Map.of(
                "model", model,
                "input", prompt,
                "text", Map.of(
                        "format", createResponseFormat()
                )
        );

        Map<?, ?> response = restClient.post()
                .uri("/responses")
                .body(body)
                .retrieve()
                .body(Map.class);

        String outputText = extractOutputText(response);

        try {
            return objectMapper.readValue(outputText, TranslationResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "OpenAI JSON 응답을 변환하지 못했습니다. 응답: " + outputText,
                    e
            );
        }
    }

    private String buildPrompt(TranslationRequest request) {
        return """
            너는 문화적 맥락을 고려하는 글로벌 커뮤니케이션 전문가다.

            아래 문장을 대상 국가, 관계, 상황과 말투에 맞게 분석하고 번역하라.

            원문: %s
            원문 언어: %s
            목표 언어: %s
            대상 국가: %s
            상황: %s
            관계: %s
            목적: %s
            요청 말투: %s

            작성 규칙:
            - literalTranslation은 원문의 의미와 구조를 최대한 유지한다.
            - naturalTranslation은 현지인이 실제로 사용할 자연스러운 표현으로 작성한다.
            - culturalTranslation은 대상 국가와 상황을 고려한 가장 안전한 표현으로 작성한다.
            - culturalExplanation은 왜 표현을 변경했는지 한국어로 설명한다.
            - 모든 점수는 0부터 100까지의 정수로 작성한다.
            - 위험 표현이 없다면 warnings는 빈 배열로 작성한다.
            - 문화 차이는 개인과 상황에 따라 달라질 수 있으므로 단정적으로 설명하지 않는다.
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
    private Map<String, Object> createResponseFormat() {
        Map<String, Object> warningProperties = Map.of(
                "expression", Map.of("type", "string"),
                "category", Map.of("type", "string"),
                "riskLevel", Map.of(
                        "type", "string",
                        "enum", List.of("SAFE", "CAUTION", "HIGH", "AVOID")
                ),
                "reason", Map.of("type", "string"),
                "alternativeExpression", Map.of("type", "string"),
                "startIndex", Map.of("type", "integer"),
                "endIndex", Map.of("type", "integer")
        );

        Map<String, Object> toneProperties = Map.of(
                "friendliness", scoreSchema(),
                "politeness", scoreSchema(),
                "directness", scoreSchema(),
                "aggression", scoreSchema(),
                "burden", scoreSchema(),
                "professionalism", scoreSchema(),
                "naturalness", scoreSchema()
        );

        Map<String, Object> rootProperties = Map.of(
                "literalTranslation", Map.of("type", "string"),
                "naturalTranslation", Map.of("type", "string"),
                "culturalTranslation", Map.of("type", "string"),
                "culturalExplanation", Map.of("type", "string"),
                "overallRiskLevel", Map.of(
                        "type", "string",
                        "enum", List.of("SAFE", "CAUTION", "HIGH", "AVOID")
                ),
                "toneScores", Map.of(
                        "type", "object",
                        "properties", toneProperties,
                        "required", List.copyOf(toneProperties.keySet()),
                        "additionalProperties", false
                ),
                "warnings", Map.of(
                        "type", "array",
                        "items", Map.of(
                                "type", "object",
                                "properties", warningProperties,
                                "required", List.copyOf(warningProperties.keySet()),
                                "additionalProperties", false
                        )
                )
        );

        return Map.of(
                "type", "json_schema",
                "name", "translation_analysis",
                "strict", true,
                "schema", Map.of(
                        "type", "object",
                        "properties", rootProperties,
                        "required", List.copyOf(rootProperties.keySet()),
                        "additionalProperties", false
                )
        );
    }

    private Map<String, Object> scoreSchema() {
        return Map.of(
                "type", "integer",
                "minimum", 0,
                "maximum", 100
        );
    }
}