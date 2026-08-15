package com.malgo.backend.member.service;

import com.malgo.backend.member.entity.Member;
import com.malgo.backend.member.repository.MemberRepository;
import com.malgo.backend.member.dto.MembershipStatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipService {

    private final MemberRepository memberRepository;

    public MembershipService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public void activateMembership(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 회원입니다.")
                );

        member.activateMembership();
    }

    @Transactional(readOnly = true)
    public MembershipStatusResponse getMembershipStatus(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 회원입니다.")
                );

        return new MembershipStatusResponse(
                member.isMembership(),
                member.getChatCount(),
                8
        );
    }
}