package com.malgo.backend.subscription.repository;

import com.malgo.backend.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);
}
