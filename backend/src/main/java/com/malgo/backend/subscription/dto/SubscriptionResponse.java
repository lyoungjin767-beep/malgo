package com.malgo.backend.subscription.dto;

import com.malgo.backend.subscription.entity.SubscriptionPlan;
import com.malgo.backend.subscription.entity.SubscriptionStatus;

import java.time.LocalDateTime;

public record SubscriptionResponse(
        Long id,
        Long memberId,
        SubscriptionPlan plan,
        SubscriptionStatus status,
        LocalDateTime startedAt,
        LocalDateTime expiresAt
) {
}
