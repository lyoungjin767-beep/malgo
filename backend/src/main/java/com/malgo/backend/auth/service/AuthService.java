package com.malgo.backend.auth.service;

import com.malgo.backend.auth.dto.LoginRequest;
import com.malgo.backend.auth.dto.SignupRequest;
import com.malgo.backend.member.entity.Member;
import com.malgo.backend.member.repository.MemberRepository;
import com.malgo.backend.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionService subscriptionService;

    @Transactional
    public Long signup(SignupRequest request) {

        // 비밀번호와 비밀번호 확인이 같은지 확인
        if (!request.password().equals(request.passwordConfirm())) {
            throw new IllegalArgumentException(
                    "비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }

        // 아이디 중복 확인
        if (memberRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 아이디입니다."
            );
        }

        // 비밀번호 암호화
        String encodedPassword =
                passwordEncoder.encode(request.password());

        // 회원 생성
        Member member = new Member(
                request.username(),
                encodedPassword
        );

        Member savedMember = memberRepository.save(member);
        subscriptionService.createFreeSubscription(savedMember);

        return savedMember.getId();
    }

    public Long login(LoginRequest request) {

        Member member = memberRepository.findByUsername(request.username())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "아이디 또는 비밀번호가 올바르지 않습니다."
                        )
                );

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                member.getPassword()
        );

        if (!passwordMatches) {
            throw new IllegalArgumentException(
                    "아이디 또는 비밀번호가 올바르지 않습니다."
            );
        }

        return member.getId();
    }
}
