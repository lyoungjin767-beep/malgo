package com.malgo.backend.auth.service;

import com.malgo.backend.auth.entity.VerificationPurpose;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService {

    private final Set<String> verifiedEmails = ConcurrentHashMap.newKeySet();

    public boolean isVerified(String email, VerificationPurpose purpose) {
        return verifiedEmails.contains(key(email, purpose));
    }

    public void markVerified(String email, VerificationPurpose purpose) {
        verifiedEmails.add(key(email, purpose));
    }

    private String key(String email, VerificationPurpose purpose) {
        return purpose.name() + ":" + email.trim().toLowerCase();
    }
}
