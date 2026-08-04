package com.malgo.backend.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "email_verifications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_email_verification_email_purpose",
                        columnNames = {"email", "purpose"}
                )
        }
)
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerificationPurpose purpose;

    @Column(
            name = "verification_code",
            nullable = false,
            length = 6
    )
    private String verificationCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public EmailVerification(
            String email,
            VerificationPurpose purpose
    ) {
        this.email = email;
        this.purpose = purpose;
        this.verificationCode = "";
        this.expiresAt = LocalDateTime.now();
        this.verified = false;
        this.createdAt = LocalDateTime.now();
    }

    public void issue(
            String verificationCode,
            LocalDateTime expiresAt
    ) {
        this.verificationCode = verificationCode;
        this.expiresAt = expiresAt;
        this.verified = false;
        this.createdAt = LocalDateTime.now();
    }

    public boolean matches(String code) {
        return verificationCode.equals(code);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void completeVerification() {
        this.verified = true;
    }
}
