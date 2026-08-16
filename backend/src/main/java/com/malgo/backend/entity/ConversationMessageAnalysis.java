package com.malgo.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "conversation_message_analyses")
public class ConversationMessageAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "conversation_message_id",
            nullable = false,
            unique = true
    )
    private ConversationMessage conversationMessage;

    @Column(name = "recommended_translation", nullable = false, length = 2000)
    private String recommendedTranslation;

    @Column(name = "request_clarity", nullable = false)
    private int requestClarity;

    @Column(name = "business_tone", nullable = false)
    private int businessTone;

    @Column(name = "intent_delivery", nullable = false)
    private int intentDelivery;

    @Column(name = "cultural_appropriateness", nullable = false)
    private int culturalAppropriateness;

    @Column(nullable = false)
    private int ambiguity;

    protected ConversationMessageAnalysis() {
    }

    public ConversationMessageAnalysis(
            ConversationMessage conversationMessage,
            String recommendedTranslation,
            int requestClarity,
            int businessTone,
            int intentDelivery,
            int culturalAppropriateness,
            int ambiguity
    ) {
        this.conversationMessage = conversationMessage;
        this.recommendedTranslation = recommendedTranslation;
        this.requestClarity = requestClarity;
        this.businessTone = businessTone;
        this.intentDelivery = intentDelivery;
        this.culturalAppropriateness = culturalAppropriateness;
        this.ambiguity = ambiguity;
    }

    public Long getId() {
        return id;
    }

    public ConversationMessage getConversationMessage() {
        return conversationMessage;
    }

    public String getRecommendedTranslation() {
        return recommendedTranslation;
    }

    public int getRequestClarity() {
        return requestClarity;
    }

    public int getBusinessTone() {
        return businessTone;
    }

    public int getIntentDelivery() {
        return intentDelivery;
    }

    public int getCulturalAppropriateness() {
        return culturalAppropriateness;
    }

    public int getAmbiguity() {
        return ambiguity;
    }
}