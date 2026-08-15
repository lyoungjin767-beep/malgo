package com.malgo.backend.auth.controller;

import com.malgo.backend.auth.dto.LoginRequest;
import com.malgo.backend.auth.dto.SignupRequest;
import com.malgo.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<Long> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        Long memberId = authService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(memberId);
    }

    @PostMapping("/login")
    public ResponseEntity<Long> login(
            @Valid @RequestBody LoginRequest request
    ) {
        Long memberId = authService.login(request);

        return ResponseEntity.ok(memberId);
    }
}
