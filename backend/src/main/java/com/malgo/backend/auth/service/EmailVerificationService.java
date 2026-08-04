package com.malgo.backend.auth.service;

import com.malgo.backend.auth.entity.EmailVerification;
import com.malgo.backend.auth.entity.VerificationPurpose;
import com.malgo.backend.auth.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository verificationRepository;
    private final MailService mailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void sendCode(
            String email,
            VerificationPurpose purpose
    ) {
        String code = createVerificationCode(email, purpose);

        mailService.sendVerificationCode(email, code);
    }

    @Transactional
    public String createVerificationCode(
            String email,
            VerificationPurpose purpose
    ) {
        String code = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        EmailVerification verification =
                new EmailVerification(email, code, purpose);

        verificationRepository.save(verification);

        return code;
    }

    @Transactional
    public void verifyCode(
            String email,
            String code,
            VerificationPurpose purpose
    ) {
        EmailVerification verification =
                verificationRepository
                        .findTopByEmailAndPurposeOrderByCreatedAtDesc(
                                email,
                                purpose
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "인증번호 발송 기록이 없습니다."
                                )
                        );

        if (verification.isExpired()) {
            throw new IllegalArgumentException(
                    "인증번호가 만료되었습니다."
            );
        }

        if (!verification.getVerificationCode().equals(code)) {
            throw new IllegalArgumentException(
                    "인증번호가 올바르지 않습니다."
            );
        }

        verification.verify();
    }

    @Transactional(readOnly = true)
    public boolean isVerified(
            String email,
            VerificationPurpose purpose
    ) {
        return verificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(
                        email,
                        purpose
                )
                .filter(verification -> !verification.isExpired())
                .filter(EmailVerification::isVerified)
                .isPresent();
    }

    @Transactional
    public void consumeVerification(
            String email,
            VerificationPurpose purpose
    ) {
        EmailVerification verification =
                verificationRepository
                        .findTopByEmailAndPurposeOrderByCreatedAtDesc(
                                email,
                                purpose
                        )
                        .filter(savedVerification -> !savedVerification.isExpired())
                        .filter(EmailVerification::isVerified)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "완료된 인증 정보가 없습니다."
                                )
                        );

        verificationRepository.delete(verification);
    }
}
