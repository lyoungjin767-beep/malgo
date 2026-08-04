package com.malgo.backend.auth.service;

import com.malgo.backend.auth.entity.EmailVerification;
import com.malgo.backend.auth.entity.VerificationPurpose;
import com.malgo.backend.auth.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

    private static final int CODE_BOUND = 1_000_000;
    private static final int CODE_LENGTH = 6;
    private static final long EXPIRATION_MINUTES = 3;

    private final EmailVerificationRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String createVerificationCode(
            String email,
            VerificationPurpose purpose
    ) {
        String code = String.format(
                "%0" + CODE_LENGTH + "d",
                secureRandom.nextInt(CODE_BOUND)
        );

        LocalDateTime expiresAt =
                LocalDateTime.now()
                        .plusMinutes(EXPIRATION_MINUTES);

        EmailVerification verification =
                repository.findByEmailAndPurpose(email, purpose)
                        .orElseGet(() ->
                                new EmailVerification(email, purpose)
                        );

        verification.issue(code, expiresAt);
        repository.save(verification);

        return code;
    }

    @Transactional
    public void verifyCode(
            String email,
            String code,
            VerificationPurpose purpose
    ) {
        EmailVerification verification =
                repository.findByEmailAndPurpose(email, purpose)
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

        if (!verification.matches(code)) {
            throw new IllegalArgumentException(
                    "인증번호가 올바르지 않습니다."
            );
        }

        verification.completeVerification();
    }

    public boolean isVerified(
            String email,
            VerificationPurpose purpose
    ) {
        return repository.findByEmailAndPurpose(email, purpose)
                .filter(EmailVerification::isVerified)
                .filter(verification -> !verification.isExpired())
                .isPresent();
    }

    @Transactional
    public void consumeVerification(
            String email,
            VerificationPurpose purpose
    ) {
        repository.findByEmailAndPurpose(email, purpose)
                .ifPresent(repository::delete);
    }
}
