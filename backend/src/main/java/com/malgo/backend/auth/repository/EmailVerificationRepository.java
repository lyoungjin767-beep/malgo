package com.malgo.backend.auth.repository;

import com.malgo.backend.auth.entity.EmailVerification;
import com.malgo.backend.auth.entity.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository
        extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmailAndPurpose(
            String email,
            VerificationPurpose purpose
    );

    Optional<EmailVerification>
    findTopByEmailAndPurposeOrderByCreatedAtDesc(
            String email,
            VerificationPurpose purpose
    );
}
