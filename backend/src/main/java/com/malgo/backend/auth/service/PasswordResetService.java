package com.malgo.backend.auth.service;

import com.malgo.backend.auth.dto.PasswordResetRequest;
import com.malgo.backend.auth.dto.PasswordResetSendRequest;
import com.malgo.backend.auth.dto.PasswordResetVerifyRequest;
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
public class PasswordResetService {

    private final MemberRepository memberRepository;
    private final EmailVerificationService emailVerificationService;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 비밀번호 재설정 인증번호 발송
     */
    @Transactional
    public void sendResetCode(PasswordResetSendRequest request) {

        if (!memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                    "가입되지 않은 이메일입니다."
            );
        }

        String verificationCode =
                emailVerificationService.createVerificationCode(
                        request.email(),
                        VerificationPurpose.PASSWORD_RESET
                );

        mailService.sendPasswordResetVerificationCode(
                request.email(),
                verificationCode
        );
    }

    /**
     * 비밀번호 재설정 인증번호 확인
     */
    @Transactional
    public void verifyResetCode(
            PasswordResetVerifyRequest request
    ) {
        emailVerificationService.verifyCode(
                request.email(),
                request.verificationCode(),
                VerificationPurpose.PASSWORD_RESET
        );
    }

    /**
     * 새 비밀번호로 변경
     */
    @Transactional
    public void resetPassword(PasswordResetRequest request) {

        if (!request.newPassword()
                .equals(request.newPasswordConfirm())) {

            throw new IllegalArgumentException(
                    "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }

        boolean verified =
                emailVerificationService.isVerified(
                        request.email(),
                        VerificationPurpose.PASSWORD_RESET
                );

        if (!verified) {
            throw new IllegalArgumentException(
                    "비밀번호 재설정 이메일 인증을 먼저 완료해주세요."
            );
        }

        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "가입되지 않은 이메일입니다."
                        )
                );

        String encodedPassword =
                passwordEncoder.encode(request.newPassword());

        member.changePassword(encodedPassword);

        emailVerificationService.consumeVerification(
                request.email(),
                VerificationPurpose.PASSWORD_RESET
        );
    }
}
