package com.malgo.backend.dto;

public record ConversationAiResult(
        String recommendedTranslation,
        int requestClarity,
        int businessTone,
        int intentDelivery,
        int culturalAppropriateness,
        int ambiguity
) {
}