package com.malgo.backend.customization.dto;

import com.malgo.backend.customization.entity.AiPersona;
import com.malgo.backend.customization.entity.ExpressionType;
import com.malgo.backend.customization.entity.GenderType;
import com.malgo.backend.customization.entity.LanguageType;
import com.malgo.backend.customization.entity.RelationshipType;
import com.malgo.backend.customization.entity.SpeechStyle;

import java.util.Set;

public record CustomizationResponse(

        Long id,

        Long memberId,

        AiPersona aiPersona,

        ExpressionType expression,

        LanguageType targetLanguage,

        Set<RelationshipType> relationships,

        GenderType gender,

        Set<SpeechStyle> speechStyles

) {
}
