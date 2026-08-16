package com.malgo.backend.member.dto;

public record MembershipStatusResponse(
        boolean membership,
        int chatCount,
        int freeChatLimit
) {
}