package com.malgo.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @NotBlank
        @Size(max = 50)
        String nickname
) {

    public String normalizedEmail() {
        return email.trim().toLowerCase();
    }

    public String normalizedNickname() {
        return nickname.trim();
    }
}
