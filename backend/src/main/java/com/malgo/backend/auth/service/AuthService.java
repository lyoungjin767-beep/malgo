package com.malgo.backend.auth.service;

import com.malgo.backend.auth.dto.LoginRequest;
import com.malgo.backend.auth.dto.SignupRequest;
import com.malgo.backend.member.entity.Member;
import com.malgo.backend.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Long signup(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new IllegalArgumentException(
                    "비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }

        if (memberRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 아이디입니다."
            );
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        Member member = new Member(
                request.username(),
                encodedPassword
        );

        return memberRepository.save(member).getId();
    }

    @Transactional(readOnly = true)
    public Long login(LoginRequest request) {
        Member member = memberRepository
                .findByUsername(request.username())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "아이디 또는 비밀번호가 올바르지 않습니다."
                        )
                );

        if (!passwordEncoder.matches(
                request.password(),
                member.getPassword()
        )) {
            throw new IllegalArgumentException(
                    "아이디 또는 비밀번호가 올바르지 않습니다."
            );
        }

        return member.getId();
    }
}
