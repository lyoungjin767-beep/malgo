package com.malgo.backend.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "email_verifications")
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "verification_code", nullable = false, length = 6)
    private String verificationCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerificationPurpose purpose;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public EmailVerification(
            String email,
            String verificationCode,
            VerificationPurpose purpose
    ) {
        this.email = email;
        this.verificationCode = verificationCode;
        this.purpose = purpose;
        this.verified = false;
        this.expiresAt = LocalDateTime.now().plusMinutes(3);
        this.createdAt = LocalDateTime.now();
    }

    public void verify() {
        this.verified = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
