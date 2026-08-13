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

    // 선택된 AI 상대의 정보와 사용자의 메시지를 바탕으로 대화용 AI 응답을 생성
    public String chat(
            String partnerName,
            String targetCountry,
            String relationshipType,
            String speechStyle,
            String characteristic,
            String situation,
            String userMessage
    ) {

        String prompt = """
            너는 Malgo의 AI 글로벌 커뮤니케이션 도우미다.

            사용자는 특정 상대방에게 어떻게 말하면 좋을지 질문하고 있다.
            아래 상대방의 정보와 상황을 고려해서 사용자에게 자연스럽게 답변하라.

            AI 상대 이름: %s
            대상 국가: %s
            사용자와 상대방의 관계: %s
            상대방 말투/스타일: %s
            상대방 특징: %s
            현재 상황: %s

            사용자 메시지:
            %s

            응답 규칙:
            - 사용자가 어떤 표현을 어떻게 말하면 좋을지 묻는 경우 실제 사용할 수 있는 표현을 추천한다.
            - 대상 국가와 관계를 고려한다.
            - 사용자의 의도를 임의로 바꾸지 않는다.
            - 원문에 없는 구체적인 사실을 만들어내지 않는다.
            - 답변은 한국어로 설명한다.
            - 필요하면 추천 번역 문장을 함께 제공한다.
            - 너무 길게 설명하지 말고 실제 채팅처럼 자연스럽게 답한다.
            """
                .formatted(
                        partnerName,
                        targetCountry,
                        relationshipType,
                        speechStyle,
                        characteristic == null ? "별도 정보 없음" : characteristic,
                        situation,
                        userMessage
                );

        Map<String, Object> body = Map.of(
                "model", model,
                "input", prompt
        );

        Map<?, ?> response = restClient.post()
                .uri("/responses")
                .body(body)
                .retrieve()
                .body(Map.class);

        // 기존에 만들어둔 응답 텍스트 추출 메서드를 그대로 사용
        return extractOutputText(response);
    }

    // 대화방의 전체 메시지를 바탕으로 핵심 대화 내용을 간단하게 요약
    public String summarizeConversation(String conversationText) {

        String prompt = """
            너는 Malgo의 대화 요약 도우미다.

            아래 대화 내용을 읽고 사용자가 나중에 다시 확인하기 쉽도록
            핵심 내용만 간결하게 요약하라.

            대화 내용:
            %s

            요약 규칙:
            - 대화의 핵심 상황과 목적을 중심으로 요약한다.
            - 중요한 요청, 결정, 약속, 거절, 일정 등이 있다면 포함한다.
            - 원문에 없는 사실을 임의로 추가하지 않는다.
            - 중복되는 내용은 제거한다.
            - 한국어로 자연스럽게 작성한다.
            - 2~4문장 정도로 간결하게 작성한다.
            """
                .formatted(conversationText);

        Map<String, Object> body = Map.of(
                "model", model,
                "input", prompt
        );

        Map<?, ?> response = restClient.post()
                .uri("/responses")
                .body(body)
                .retrieve()
                .body(Map.class);

        return extractOutputText(response);
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
            - culturalTranslation은 대상 국가, 관계, 상황을 고려한 가장 적절한 표현으로 작성한다.
            - culturalExplanation과 warning의 reason은 반드시 sourceLanguage와 동일한 언어로 작성한다.
            - sourceLanguage가 ko이면 설명과 reason은 반드시 한국어로 작성한다.
            - sourceLanguage가 en이면 설명과 reason은 반드시 영어로 작성한다.
            - sourceLanguage가 ja이면 설명과 reason은 반드시 일본어로 작성한다.
            - targetLanguage는 번역 결과와 alternativeExpression에만 사용하며, 설명 언어를 결정하는 데 사용하지 않는다.
            - culturalExplanation은 핵심적인 문화적 차이와 수정 이유만 2~4문장으로 간결하게 작성한다.
            - 모든 점수는 0부터 100까지의 정수로 작성한다.
            
            - warnings에는 실제로 오해, 무례함, 압박감, 공격성, 문화적 민감성 등의 문제가 발생할 가능성이 있는 표현만 포함한다.
            - 단순히 표현이 모호하거나 더 구체적으로 쓸 수 있다는 이유만으로 CAUTION을 부여하지 않는다.
            - 위험 표현이 없다면 warnings는 빈 배열로 반환한다.
            - 동일한 원인에 대한 경고를 중복해서 생성하지 않는다.
            
            - warning의 expression은 반드시 originalText에 실제로 존재하는 문자열이어야 한다.
            - startIndex는 originalText에서 expression이 시작되는 0-based 문자 위치이다.
            - endIndex는 expression의 마지막 문자 다음 위치(exclusive)이다.
            - 따라서 originalText.substring(startIndex, endIndex)의 결과가 expression과 정확히 일치해야 한다.
            
            - warning category는 다음 중 하나만 사용한다:
              DIRECTNESS, POLITENESS, PRESSURE, FORMALITY,
              PERSONAL_ATTACK, SARCASM, CULTURAL_TABOO,
              AMBIGUITY, SENSITIVITY, OTHER
              
            - 상대방의 능력, 성격, 지능, 인격 등을 직접적으로 깎아내리는 표현은 PERSONAL_ATTACK으로 분류한다.
            - 표면적인 의미와 실제 의도가 반대이거나, 칭찬 형태로 불만·조롱·비난을 전달하는 표현은 SARCASM으로 분류한다.
            
            - alternativeExpression은 targetLanguage로 작성한다.
            - SAFE는 실질적인 문화적 또는 커뮤니케이션 위험이 없는 경우 사용한다.
            - CAUTION은 상황이나 관계에 따라 부정적으로 받아들여질 가능성이 있는 경우 사용한다.
            - HIGH는 오해, 무례함 또는 관계 악화 가능성이 높은 경우 사용한다.
            - AVOID는 대상 문화나 상황에서 사용하지 않는 것이 강하게 권장되는 경우 사용한다.
            - AVOID는 단순히 강하거나 무례한 표현이라는 이유만으로 사용하지 않는다.
            - AVOID는 심각한 모욕, 차별, 혐오, 문화적 금기 등 대상 문화나 관계에서 그대로 사용하는 것을 강하게 피해야 하는 표현에 사용한다.
            - 강한 압박, 최후통첩, 관계 악화 가능성이 높더라도 위 기준에 해당하지 않는다면 HIGH를 사용한다.
            - PERSONAL_ATTACK의 alternativeExpression은 사람의 능력, 성격, 지능 또는 인격을 평가하지 않는다.
            - 개인에 대한 비판은 가능한 경우 행동, 결과, 업무 품질 또는 구체적인 문제에 대한 표현으로 전환한다.
            - 문화 차이는 개인과 상황에 따라 다를 수 있으므로 고정관념처럼 단정하지 않는다.
            - culturalExplanation과 warning의 reason은 sourceLanguage 사용자가 이해할 수 있는 언어로 작성한다.
            
            - culturalTranslation과 alternativeExpression은 원문의 핵심 의도와 감정을 유지해야 한다.
            - 문화적으로 부적절한 표현을 완화하더라도 사용자의 불만, 요청, 거절, 긴급성 등의 핵심 의도를 임의로 삭제하거나 반대 의미로 변경하지 않는다.
            - 비꼼이나 반어 표현을 완화할 때는 숨겨진 실제 의도를 명확하고 건설적인 표현으로 변환한다.
            
            - 거절 표현을 문화적으로 조정할 때는 거절 의도를 명확히 유지한다.
            - 원문에 대안이나 이유가 없는 경우 구체적인 이유나 대안을 임의로 만들어내지 않는다.
            - 다만 관계를 유지하기 위한 중립적인 후속 표현은 필요할 경우 최소한으로 추가할 수 있다.
            
            - 원문에 존재하지 않는 구체적인 사실, 대상, 이유, 일정, 인물, 장소 등의 정보를 임의로 추측하거나 추가하지 않는다.
            - 원문에서 대상이 모호한 경우에도 문맥 정보가 제공되지 않았다면 임의로 구체화하지 않는다.
            - 예를 들어 "this"를 근거 없이 "document", "file", "資料" 등으로 바꾸지 않는다.
            - culturalTranslation은 문화적으로 자연스럽게 조정하되 원문의 정보 범위를 유지한다.
        
            - toneScores는 culturalTranslation이나 수정된 문장이 아니라 originalText의 표현을 평가한다.
            - 단, 단순한 언어적 특성이 아니라 입력된 situation, relationshipType, targetCountry를 고려하여 상대방이 originalText의 의도를 어떻게 받아들일 가능성이 있는지를 기준으로 평가한다.
            
            - overallRiskLevel은 문장 전체의 커뮤니케이션 위험도를 나타낸다.
            - warnings가 하나 이상 존재하는 경우 overallRiskLevel은 원칙적으로 warnings 중 가장 높은 riskLevel보다 낮을 수 없다.
            - 위험도 우선순위는 SAFE < CAUTION < HIGH < AVOID 순서이다.
            
            - 특정 단어나 표현이 포함되었다는 이유만으로 자동으로 warning을 생성하지 않는다.
            - warning은 해당 표현이 현재의 문맥, 상황, 관계, 대상 국가에서 실제로 커뮤니케이션 위험을 만드는 경우에만 생성한다.
            - 동일한 표현이라도 문맥에 따라 SAFE일 수 있음을 고려한다.
            
            - warning 판단은 표현 자체의 격식이나 직설성만으로 결정하지 않고 situation, relationshipType, requestedTone을 반드시 함께 고려한다.
            - FRIEND + DAILY + CASUAL과 같이 친밀하고 비격식적인 상황에서는 일상적인 반말, 가벼운 농담, 웃음 표현, 친근한 호칭 등을 그 자체만으로 위험 표현으로 판단하지 않는다.
            - targetLanguage에서 자연스럽게 표현만 바꾸면 해결되는 단순한 언어·문화 차이는 warning을 생성하지 않고 naturalTranslation 또는 culturalTranslation에서 자연스럽게 조정한다.
            - warning은 실제로 상대방에게 오해, 불쾌감, 압박, 모욕, 관계 악화 등의 의미 있는 위험이 예상될 때만 생성한다.
            - 사소하거나 가능성이 낮은 위험까지 과도하게 경고하지 않는다.
            
            - 원문에 이름이나 호칭이 없는 경우 [Name], [Client Name] 등의 placeholder도 임의로 추가하지 않는다.
            
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
                "category", Map.of(
                        "type", "string",
                        "enum", List.of(
                                "DIRECTNESS",
                                "POLITENESS",
                                "PRESSURE",
                                "FORMALITY",
                                "PERSONAL_ATTACK",
                                "SARCASM",
                                "CULTURAL_TABOO",
                                "AMBIGUITY",
                                "SENSITIVITY",
                                "OTHER"
                        )
                ),
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