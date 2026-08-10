package com.malgo.backend.service;

import com.malgo.backend.dto.AiPartnerResponse;
import com.malgo.backend.repository.AiPartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


// AI 대화 상대 관련 기능을 처리하는 서비스

@Service
public class AiPartnerService {

    private final AiPartnerRepository aiPartnerRepository;

    public AiPartnerService(
            AiPartnerRepository aiPartnerRepository
    ) {
        this.aiPartnerRepository = aiPartnerRepository;
    }

    // 저장된 AI 대화 상대 목록을 조회
    @Transactional(readOnly = true)
    public List<AiPartnerResponse> getPartners() {

        return aiPartnerRepository.findAll()
                .stream()
                .map(partner -> new AiPartnerResponse(
                        partner.getId(),
                        partner.getName(),
                        partner.getTargetCountry(),
                        partner.getRelationshipType(),
                        partner.getAgeGroup(),
                        partner.getGender(),
                        partner.getSpeechStyle(),
                        partner.getCharacteristic(),
                        partner.isCustom()
                ))
                .toList();
    }
}