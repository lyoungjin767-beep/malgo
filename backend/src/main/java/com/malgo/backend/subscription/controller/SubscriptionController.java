package com.malgo.backend.subscription.controller;

import com.malgo.backend.subscription.dto.SubscriptionResponse;
import com.malgo.backend.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/me")
    public ResponseEntity<SubscriptionResponse> getMySubscription(
            @RequestHeader("X-Member-Id") Long memberId
    ) {
        return ResponseEntity.ok(subscriptionService.getMySubscription(memberId));
    }

    @PatchMapping("/me/premium")
    public ResponseEntity<SubscriptionResponse> activatePremium(
            @RequestHeader("X-Member-Id") Long memberId
    ) {
        return ResponseEntity.ok(subscriptionService.activatePremium(memberId));
    }

    @PatchMapping("/me/cancel")
    public ResponseEntity<SubscriptionResponse> cancel(
            @RequestHeader("X-Member-Id") Long memberId
    ) {
        return ResponseEntity.ok(subscriptionService.cancel(memberId));
    }
}
