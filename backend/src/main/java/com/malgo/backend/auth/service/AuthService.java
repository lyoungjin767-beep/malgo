package com.malgo.backend.auth.service;

import com.malgo.backend.auth.dto.LoginRequest;
import com.malgo.backend.auth.dto.SignupRequest;
import com.malgo.backend.member.entity.Member;
import com.malgo.backend.member.repository.MemberRepository;
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

    @Transactional
    public Long signup(SignupRequest request) {

        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                    "이미 가입된 이메일입니다."
            );
        }

        String encodedPassword =
                passwordEncoder.encode(request.password());

        Member member = new Member(
                request.email(),
                encodedPassword,
                request.nickname()
        );

        Member savedMember = memberRepository.save(member);

        return savedMember.getId();
    }

    public Long login(LoginRequest request) {

        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "이메일 또는 비밀번호가 올바르지 않습니다."
                        )
                );

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                member.getPassword()
        );

        if (!passwordMatches) {
            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }

        return member.getId();
    }
}
