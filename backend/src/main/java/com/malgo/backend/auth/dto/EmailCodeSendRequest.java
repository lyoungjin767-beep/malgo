package com.malgo.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailCodeSendRequest(

        @NotBlank
        @Email
        String email
) {
}
