package com.malgo.backend.subscription.entity;

import com.malgo.backend.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public Subscription(
            Member member,
            SubscriptionPlan plan,
            SubscriptionStatus status,
            LocalDateTime startedAt,
            LocalDateTime expiresAt
    ) {
        this.member = member;
        this.plan = plan;
        this.status = status;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }

    public static Subscription free(Member member) {
        return new Subscription(
                member,
                SubscriptionPlan.FREE,
                SubscriptionStatus.ACTIVE,
                LocalDateTime.now(),
                null
        );
    }

    public void activatePremium(LocalDateTime expiresAt) {
        this.plan = SubscriptionPlan.PREMIUM;
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELED;
    }
}
