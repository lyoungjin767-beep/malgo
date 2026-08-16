package com.malgo.backend.customization.entity;

import com.malgo.backend.member.entity.Member;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_customizations")
public class UserCustomization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_persona", nullable = false, length = 20)
    private AiPersona aiPersona;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpressionType expression;

    // 사용 언어
    @Enumerated(EnumType.STRING)
    @Column(name = "target_language", nullable = false, length = 10)
    private LanguageType targetLanguage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GenderType gender;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "user_customization_relationships",
            joinColumns = @JoinColumn(name = "customization_id")
    )
    @Column(name = "relationship", nullable = false, length = 30)
    private Set<RelationshipType> relationships = new HashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "user_customization_speech_styles",
            joinColumns = @JoinColumn(name = "customization_id")
    )
    @Column(name = "speech_style", nullable = false, length = 30)
    private Set<SpeechStyle> speechStyles = new HashSet<>();

    public UserCustomization(
            Member member,
            AiPersona aiPersona,
            ExpressionType expression,
            LanguageType targetLanguage,
            Set<RelationshipType> relationships,
            GenderType gender,
            Set<SpeechStyle> speechStyles
    ) {
        this.member = member;

        update(
                aiPersona,
                expression,
                targetLanguage,
                relationships,
                gender,
                speechStyles
        );
    }

    public void update(
            AiPersona aiPersona,
            ExpressionType expression,
            LanguageType targetLanguage,
            Set<RelationshipType> relationships,
            GenderType gender,
            Set<SpeechStyle> speechStyles
    ) {
        this.aiPersona = aiPersona;
        this.expression = expression;
        this.targetLanguage = targetLanguage;
        this.relationships = new HashSet<>(relationships);
        this.gender = gender;
        this.speechStyles = new HashSet<>(speechStyles);
    }
}
