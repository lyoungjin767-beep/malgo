package com.malgo.backend.service;

import com.malgo.backend.ai.OpenAiClient;
import com.malgo.backend.dto.TranslationRequest;
import com.malgo.backend.dto.TranslationResponse;
import org.springframework.stereotype.Service;

@Service
public class TranslationService {

    private final OpenAiClient openAiClient;

    public TranslationService(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    public TranslationResponse analyze(TranslationRequest request) {
        return openAiClient.translate(request);
    }
}