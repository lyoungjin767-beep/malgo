package com.malgo.backend.auth.service;

import com.malgo.backend.auth.dto.LoginRequest;
import com.malgo.backend.auth.dto.PasswordResetRequest;
import com.malgo.backend.auth.dto.SignupRequest;
import com.malgo.backend.auth.entity.VerificationPurpose;
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
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public Long signup(SignupRequest request) {

        if (!request.password().equals(request.passwordConfirm())) {
            throw new IllegalArgumentException(
                    "비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }

        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                    "이미 가입된 이메일입니다."
            );
        }

        boolean verified =
                emailVerificationService.isVerified(
                        request.email(),
                        VerificationPurpose.SIGNUP
                );

        if (!verified) {
            throw new IllegalArgumentException(
                    "이메일 인증을 먼저 완료해주세요."
            );
        }

        String encodedPassword =
                passwordEncoder.encode(request.password());

        Member member = new Member(
                request.email(),
                encodedPassword,
                request.name()
        );

        return memberRepository.save(member).getId();
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

    @Transactional
    public void resetPassword(PasswordResetRequest request) {

        if (!request.newPassword()
                .equals(request.newPasswordConfirm())) {
            throw new IllegalArgumentException(
                    "새 비밀번호가 일치하지 않습니다."
            );
        }

        boolean verified =
                emailVerificationService.isVerified(
                        request.email(),
                        VerificationPurpose.PASSWORD_RESET
                );

        if (!verified) {
            throw new IllegalArgumentException(
                    "비밀번호 재설정 인증을 먼저 완료해주세요."
            );
        }

        Member member = memberRepository
                .findByEmail(request.email())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "가입되지 않은 이메일입니다."
                        )
                );

        String encodedPassword =
                passwordEncoder.encode(request.newPassword());

        member.changePassword(encodedPassword);
    }
}
