package com.malgo.backend.auth.controller;

import com.malgo.backend.auth.dto.LoginRequest;
import com.malgo.backend.auth.dto.EmailCodeSendRequest;
import com.malgo.backend.auth.dto.EmailCodeVerifyRequest;
import com.malgo.backend.auth.dto.SignupRequest;
import com.malgo.backend.auth.dto.PasswordResetRequest;
import com.malgo.backend.auth.dto.PasswordResetSendRequest;
import com.malgo.backend.auth.dto.PasswordResetVerifyRequest;
import com.malgo.backend.auth.entity.VerificationPurpose;
import com.malgo.backend.auth.service.AuthService;
import com.malgo.backend.auth.service.EmailVerificationService;
import com.malgo.backend.auth.service.MailService;
import com.malgo.backend.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final MailService mailService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/email/send")
    public ResponseEntity<Map<String, String>> sendSignupCode(
            @Valid @RequestBody EmailCodeSendRequest request
    ) {
        String verificationCode =
                emailVerificationService.createVerificationCode(
                        request.email(),
                        VerificationPurpose.SIGNUP
                );

        mailService.sendVerificationCode(
                request.email(),
                verificationCode
        );

        return ResponseEntity.ok(
                Map.of("message", "인증번호를 전송했습니다.")
        );
    }

    @PostMapping("/email/verify")
    public ResponseEntity<Map<String, String>> verifySignupCode(
            @Valid @RequestBody EmailCodeVerifyRequest request
    ) {
        emailVerificationService.verifyCode(
                request.email(),
                request.code(),
                VerificationPurpose.SIGNUP
        );

        return ResponseEntity.ok(
                Map.of("message", "이메일 인증에 성공했습니다.")
        );
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        Long memberId = authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "회원가입에 성공했습니다.",
                        "memberId", memberId
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        Long memberId = authService.login(request);

        return ResponseEntity.ok(
                Map.of(
                        "message", "로그인에 성공했습니다.",
                        "memberId", memberId
                )
        );
    }

    @PostMapping("/password/reset/send")
    public ResponseEntity<Map<String, String>> sendPasswordResetCode(
            @Valid @RequestBody PasswordResetSendRequest request
    ) {
        passwordResetService.sendResetCode(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "비밀번호 재설정 인증번호를 발송했습니다."
                )
        );
    }

    @PostMapping("/password/reset/verify")
    public ResponseEntity<Map<String, String>> verifyPasswordResetCode(
            @Valid @RequestBody PasswordResetVerifyRequest request
    ) {
        passwordResetService.verifyResetCode(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "비밀번호 재설정 이메일 인증에 성공했습니다."
                )
        );
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        passwordResetService.resetPassword(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "비밀번호가 변경되었습니다."
                )
        );
    }
}
