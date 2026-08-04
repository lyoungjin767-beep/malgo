package com.example.backend.auth.dto;

import com.example.backend.member.entity.Member;

public record AuthResponse(
        Long id,
        String email,
        String nickname
) {

    public static AuthResponse from(Member member) {
        return new AuthResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname()
        );
    }
}
