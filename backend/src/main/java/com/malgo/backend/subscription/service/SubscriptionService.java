package com.malgo.backend.subscription.service;

import com.malgo.backend.member.entity.Member;
import com.malgo.backend.member.repository.MemberRepository;
import com.malgo.backend.subscription.dto.SubscriptionResponse;
import com.malgo.backend.subscription.entity.Subscription;
import com.malgo.backend.subscription.entity.SubscriptionPlan;
import com.malgo.backend.subscription.entity.SubscriptionStatus;
import com.malgo.backend.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Subscription createFreeSubscription(Member member) {
        if (subscriptionRepository.existsByMemberId(member.getId())) {
            return subscriptionRepository.findByMemberId(member.getId())
                    .orElseThrow(() -> new IllegalStateException("구독 정보를 찾을 수 없습니다."));
        }

        return subscriptionRepository.save(Subscription.free(member));
    }

    @Transactional
    public SubscriptionResponse getMySubscription(Long memberId) {
        Member member = getMember(memberId);
        Subscription subscription = getOrCreateSubscription(member);

        return toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse activatePremium(Long memberId) {
        Member member = getMember(memberId);
        Subscription subscription = getOrCreateSubscription(member);

        subscription.activatePremium(null);

        return toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse cancel(Long memberId) {
        Member member = getMember(memberId);
        Subscription subscription = getOrCreateSubscription(member);

        subscription.cancel();

        return toResponse(subscription);
    }

    @Transactional(readOnly = true)
    public boolean isPremium(Member member) {
        return subscriptionRepository.findByMemberId(member.getId())
                .filter(subscription -> subscription.getPlan() == SubscriptionPlan.PREMIUM)
                .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
                .filter(subscription -> subscription.getExpiresAt() == null
                        || subscription.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    private Subscription getOrCreateSubscription(Member member) {
        return subscriptionRepository.findByMemberId(member.getId())
                .orElseGet(() -> subscriptionRepository.save(Subscription.free(member)));
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "회원을 찾을 수 없습니다. id=" + memberId
                ));
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getMember().getId(),
                subscription.getPlan(),
                subscription.getStatus(),
                subscription.getStartedAt(),
                subscription.getExpiresAt()
        );
    }
}
