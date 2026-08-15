package com.malgo.backend.auth.service;

import com.malgo.backend.exception.AccessDeniedException;
import com.malgo.backend.member.entity.Member;
import com.malgo.backend.member.repository.MemberRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberAuthorizationService {

    private final MemberRepository memberRepository;

    public MemberAuthorizationService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public void validateMember(Long requestedMemberId) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }

        Member authenticatedMember = memberRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "로그인한 회원 정보를 찾을 수 없습니다."
                        )
                );

        if (!authenticatedMember.getId().equals(requestedMemberId)) {
            throw new AccessDeniedException(
                    "로그인한 회원과 요청 회원이 일치하지 않습니다."
            );
        }
    }
}
