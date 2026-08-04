package com.example.project.service;

import com.example.project.dto.CultureWarningResponse;
import com.example.project.dto.ToneScores;
import com.example.project.dto.TranslationRequest;
import com.example.project.dto.TranslationResponse;
import org.springframework.stereotype.Service;

import java.util.List;

// 지금은 openai를 호출하지 않고 가짜 응답을 반환하는 Mock 서비스

@Service
public class TranslationService {

    public TranslationResponse analyze(TranslationRequest request) {
        CultureWarningResponse warning = new CultureWarningResponse(
                "Please reply quickly.",
                "DIRECT_COMMAND",
                "CAUTION",
                "비즈니스 상황에서는 재촉하거나 명령하는 표현처럼 느껴질 수 있습니다.",
                "I would appreciate your response at your earliest convenience.",
                0,
                21
        );

        ToneScores toneScores = new ToneScores(
                45,
                90,
                35,
                5,
                20,
                90,
                95
        );

        return new TranslationResponse(
                "Please reply quickly.",
                "Please get back to me when possible.",
                "I would appreciate your response at your earliest convenience.",
                "직접적인 요청을 영어권 비즈니스 상황에 맞게 완곡하고 정중한 표현으로 조정했습니다.",
                "CAUTION",
                toneScores,
                List.of(warning)
        );
    }
}