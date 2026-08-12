package com.malgo.backend.service;

import com.malgo.backend.dto.AiPartnerResponse;
import com.malgo.backend.repository.AiPartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.malgo.backend.dto.AiPartnerCreateRequest;
import com.malgo.backend.dto.AiPartnerUpdateRequest;
import com.malgo.backend.entity.AiPartner;

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

    /**
     * AI 메이커에서 새로운 커스텀 AI 상대를 생성한다.
     */
    @Transactional
    public AiPartnerResponse createCustomPartner(
            AiPartnerCreateRequest request
    ) {

        AiPartner partner = new AiPartner(
                request.name(),
                request.targetCountry(),
                request.relationshipType(),
                request.ageGroup(),
                request.gender(),
                request.speechStyle(),
                request.characteristic(),

                // 사용자가 직접 만든 상대이므로 true
                true
        );

        AiPartner saved =
                aiPartnerRepository.save(partner);

        return new AiPartnerResponse(
                saved.getId(),
                saved.getName(),
                saved.getTargetCountry(),
                saved.getRelationshipType(),
                saved.getAgeGroup(),
                saved.getGender(),
                saved.getSpeechStyle(),
                saved.getCharacteristic(),
                saved.isCustom()
        );
    }


    // 커스텀 AI 상대 정보를 수정
    @Transactional
    public AiPartnerResponse updatePartner(
            Long partnerId,
            AiPartnerUpdateRequest request
    ) {

        AiPartner partner =
                aiPartnerRepository.findById(partnerId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "AI 상대를 찾을 수 없습니다. id=" + partnerId
                                )
                        );

        // 기본 제공 캐릭터는 수정하지 못하도록 막는다.
        if (!partner.isCustom()) {
            throw new IllegalArgumentException(
                    "기본 AI 상대는 수정할 수 없습니다."
            );
        }

        partner.update(
                request.name(),
                request.targetCountry(),
                request.relationshipType(),
                request.ageGroup(),
                request.gender(),
                request.speechStyle(),
                request.characteristic()
        );

        return new AiPartnerResponse(
                partner.getId(),
                partner.getName(),
                partner.getTargetCountry(),
                partner.getRelationshipType(),
                partner.getAgeGroup(),
                partner.getGender(),
                partner.getSpeechStyle(),
                partner.getCharacteristic(),
                partner.isCustom()
        );
    }
}