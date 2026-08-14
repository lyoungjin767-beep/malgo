package com.malgo.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "아이디는 필수입니다.")
        @Size(max = 30, message = "아이디는 30자 이하여야 합니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password

) {
}