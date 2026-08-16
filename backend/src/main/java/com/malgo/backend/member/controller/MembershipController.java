package com.malgo.backend.member.controller;

import com.malgo.backend.member.service.MembershipService;
import com.malgo.backend.member.dto.MembershipStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping("/{memberId}/membership")
    public ResponseEntity<Void> activateMembership(
            @PathVariable Long memberId
    ) {
        membershipService.activateMembership(memberId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{memberId}/membership")
    public ResponseEntity<MembershipStatusResponse> getMembershipStatus(
            @PathVariable Long memberId
    ) {
        MembershipStatusResponse response =
                membershipService.getMembershipStatus(memberId);

        return ResponseEntity.ok(response);
    }
}