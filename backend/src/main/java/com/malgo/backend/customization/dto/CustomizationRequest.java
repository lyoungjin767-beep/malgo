package com.malgo.backend.customization.dto;

import com.malgo.backend.customization.entity.AiPersona;
import com.malgo.backend.customization.entity.ExpressionType;
import com.malgo.backend.customization.entity.GenderType;
import com.malgo.backend.customization.entity.LanguageType;
import com.malgo.backend.customization.entity.RelationshipType;
import com.malgo.backend.customization.entity.SpeechStyle;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record CustomizationRequest(

        @NotNull
        AiPersona aiPersona,

        @NotNull
        ExpressionType expression,

        @NotNull
        LanguageType targetLanguage,

        @NotEmpty
        Set<RelationshipType> relationships,

        @NotNull
        GenderType gender,

        @NotEmpty
        Set<SpeechStyle> speechStyles

) {
}
