package com.malgo.backend.customization.service;

import com.malgo.backend.customization.dto.CustomizationRequest;
import com.malgo.backend.customization.dto.CustomizationResponse;
import com.malgo.backend.customization.entity.UserCustomization;
import com.malgo.backend.customization.repository.UserCustomizationRepository;
import com.malgo.backend.exception.AccessDeniedException;
import com.malgo.backend.member.entity.Member;
import com.malgo.backend.member.repository.MemberRepository;
import com.malgo.backend.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomizationService {

    private final UserCustomizationRepository userCustomizationRepository;
    private final MemberRepository memberRepository;
    private final SubscriptionService subscriptionService;

    @Transactional(readOnly = true)
    public CustomizationResponse getMyCustomization(Long memberId) {
        UserCustomization customization = userCustomizationRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "커스텀 정보를 찾을 수 없습니다. memberId=" + memberId
                ));

        return toResponse(customization);
    }

    @Transactional
    public CustomizationResponse updateMyCustomization(
            Long memberId,
            CustomizationRequest request
    ) {
        Member member = getMember(memberId);

        if (!subscriptionService.isPremium(member)) {
            throw new AccessDeniedException("구독 회원만 이용할 수 있습니다.");
        }

        UserCustomization customization = userCustomizationRepository.findByMemberId(memberId)
                .orElseGet(() -> new UserCustomization(
                        member,
                        request.aiPersona(),
                        request.expression(),
                        request.relationships(),
                        request.gender(),
                        request.speechStyles()
                ));

        customization.update(
                request.aiPersona(),
                request.expression(),
                request.relationships(),
                request.gender(),
                request.speechStyles()
        );

        UserCustomization saved = userCustomizationRepository.save(customization);

        return toResponse(saved);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "회원을 찾을 수 없습니다. id=" + memberId
                ));
    }

    private CustomizationResponse toResponse(UserCustomization customization) {
        return new CustomizationResponse(
                customization.getId(),
                customization.getMember().getId(),
                customization.getAiPersona(),
                customization.getExpression(),
                Set.copyOf(customization.getRelationships()),
                customization.getGender(),
                Set.copyOf(customization.getSpeechStyles())
        );
    }
}
