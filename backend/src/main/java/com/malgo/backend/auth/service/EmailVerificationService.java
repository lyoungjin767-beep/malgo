package com.malgo.backend.auth.service;

import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService {

    public boolean isVerified(String email, VerificationPurpose purpose) {
        return true;
    }
}
