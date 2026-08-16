package com.malgo.backend.service;

import com.malgo.backend.dto.AiPartnerResponse;
import com.malgo.backend.entity.Conversation;
import com.malgo.backend.exception.AccessDeniedException;
import com.malgo.backend.repository.AiPartnerRepository;
import com.malgo.backend.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.malgo.backend.dto.AiPartnerCreateRequest;
import com.malgo.backend.dto.AiPartnerUpdateRequest;
import com.malgo.backend.entity.AiPartner;
import com.malgo.backend.member.entity.Member;
import com.malgo.backend.member.repository.MemberRepository;

import java.util.List;


// AI 대화 상대 관련 기능을 처리하는 서비스

@Service
public class AiPartnerService {

    private final AiPartnerRepository aiPartnerRepository;
    private final ConversationService conversationService;
    private final ConversationRepository conversationRepository;
    private final MemberRepository memberRepository;

    public AiPartnerService(
            AiPartnerRepository aiPartnerRepository,
            ConversationRepository conversationRepository,
            ConversationService conversationService,
            MemberRepository memberRepository
    ) {
        this.aiPartnerRepository = aiPartnerRepository;
        this.conversationRepository = conversationRepository;
        this.conversationService = conversationService;
        this.memberRepository = memberRepository;
    }

    // 기본 AI + 해당 회원이 만든 커스텀 AI 목록 조회
    @Transactional(readOnly = true)
    public List<AiPartnerResponse> getPartners(Long memberId) {

        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException(
                    "회원을 찾을 수 없습니다. id=" + memberId
            );
        }

        return aiPartnerRepository.findByMemberIdOrMemberIsNull(memberId)
                .stream()
                .map(partner -> new AiPartnerResponse(
                        partner.getId(),
                        partner.getName(),
                        partner.getTargetCountry(),
                        partner.getTargetLanguage(),
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
            Long memberId,
            AiPartnerCreateRequest request
    ) {
        // 커스텀 AI를 만드는 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "회원을 찾을 수 없습니다. id=" + memberId
                        )
                );

        if (!member.isMembership()) {
            throw new IllegalStateException(
                    "커스텀 AI 생성은 멤버십이 필요합니다."
            );
        }

        AiPartner partner = new AiPartner(
                member,
                request.name(),
                request.targetCountry(),
                request.targetLanguage(),
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
                saved.getTargetLanguage(),
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
            Long memberId,
            Long partnerId,
            AiPartnerUpdateRequest request
    ) {

        AiPartner partner =
                getOwnedCustomPartner(memberId, partnerId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "회원을 찾을 수 없습니다. id=" + memberId
                        )
                );

        if (!member.isMembership()) {
            throw new IllegalStateException(
                    "커스텀 AI 수정은 멤버십이 필요합니다."
            );
        }

        partner.update(
                request.name(),
                request.targetCountry(),
                request.targetLanguage(),
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
                partner.getTargetLanguage(),
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
    public void deletePartner(Long memberId, Long partnerId) {

        // 해당 회원이 만든 커스텀 AI인지 확인하기
        AiPartner partner =
                getOwnedCustomPartner(memberId, partnerId);

        // 해당 AI 상대와 연결된 모든 대화방 조회
        List<Conversation> conversations =
                conversationRepository.findByAiPartnerId(partnerId);

        // 연결된 대화방과 메시지/요약 먼저 삭제
        for (Conversation conversation : conversations) {
            conversationService.deleteConversation(
                    memberId,
                    conversation.getId()
            );
        }

        // 마지막으로 AI 상대 삭제
        aiPartnerRepository.delete(partner);
    }

    // AI 상대 1명의 상세 정보를 조회
    @Transactional(readOnly = true)
    public AiPartnerResponse getPartner(
            Long memberId,
            Long partnerId
    ) {

        AiPartner partner = aiPartnerRepository.findById(partnerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "AI 상대를 찾을 수 없습니다. id=" + partnerId
                        )
                );

        // 커스텀 AI인 경우에는 소유자 확인
        if (partner.isCustom()) {
            if (partner.getMember() == null
                    || !partner.getMember().getId().equals(memberId)) {
                throw new AccessDeniedException(
                        "해당 회원의 AI 상대가 아닙니다."
                );
            }
        }

        return new AiPartnerResponse(
                partner.getId(),
                partner.getName(),
                partner.getTargetCountry(),
                partner.getTargetLanguage(),
                partner.getRelationshipType(),
                partner.getAgeGroup(),
                partner.getGender(),
                partner.getSpeechStyle(),
                partner.getCharacteristic(),
                partner.isCustom()
        );
    }

    // 다른 회원의 AI는 보이지 않도록
    private AiPartner getOwnedCustomPartner(
            Long memberId,
            Long partnerId
    ) {

        AiPartner partner = aiPartnerRepository.findById(partnerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "AI 상대를 찾을 수 없습니다. id=" + partnerId
                        )
                );

        // 기본 제공 AI는 회원 소유가 아님
        if (!partner.isCustom()) {
            throw new AccessDeniedException(
                    "기본 AI 상대는 수정하거나 삭제할 수 없습니다."
            );
        }

        // 커스텀 AI인데 회원 정보가 없거나 다른 회원의 AI인 경우 차단
        if (partner.getMember() == null
                || !partner.getMember().getId().equals(memberId)) {

            throw new AccessDeniedException(
                    "해당 회원의 AI 상대가 아닙니다."
            );
        }

        return partner;
    }
}
