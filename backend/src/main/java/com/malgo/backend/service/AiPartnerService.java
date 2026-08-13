package com.malgo.backend.service;

import com.malgo.backend.dto.AiPartnerResponse;
import com.malgo.backend.entity.Conversation;
import com.malgo.backend.repository.AiPartnerRepository;
import com.malgo.backend.repository.ConversationRepository;
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
    private final ConversationService conversationService;
    private final ConversationRepository conversationRepository;

    public AiPartnerService(
            AiPartnerRepository aiPartnerRepository,
            ConversationRepository conversationRepository,
            ConversationService conversationService
    ) {
        this.aiPartnerRepository = aiPartnerRepository;
        this.conversationRepository = conversationRepository;
        this.conversationService = conversationService;
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

    // AI 메이커에서 새로운 커스텀 AI 상대를 생성
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

    // 사용자가 직접 만든 커스텀 AI 상대를 삭제
    // 기본 제공 AI 상대(Tom, kash, sana)는 삭제할 수 없음
    // 연결된 대화방이 있다면 대화방과 메시지/요약도 함께 삭제
    @Transactional
    public void deletePartner(Long partnerId) {

        AiPartner partner = aiPartnerRepository.findById(partnerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "AI 상대를 찾을 수 없습니다. id=" + partnerId
                        )
                );

        // 기본 제공 AI는 삭제 불가
        if (!partner.isCustom()) {
            throw new IllegalArgumentException(
                    "기본 AI 상대는 삭제할 수 없습니다."
            );
        }

        // 해당 AI 상대와 연결된 모든 대화방 조회
        List<Conversation> conversations =
                conversationRepository.findByAiPartnerId(partnerId);

        // 연결된 대화방과 메시지/요약 먼저 삭제
        for (Conversation conversation : conversations) {
            conversationService.deleteConversation(
                    conversation.getId()
            );
        }

        // 마지막으로 AI 상대 삭제
        aiPartnerRepository.delete(partner);
    }

    // AI 상대 1명의 상세 정보를 조회
    @Transactional(readOnly = true)
    public AiPartnerResponse getPartner(Long partnerId) {

        AiPartner partner = aiPartnerRepository.findById(partnerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "AI 상대를 찾을 수 없습니다. id=" + partnerId
                        )
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