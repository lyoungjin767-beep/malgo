package com.malgo.backend.auth.service;

import com.malgo.backend.auth.dto.LoginRequest;
import com.malgo.backend.auth.dto.SignupRequest;
import com.malgo.backend.member.entity.Member;
import com.malgo.backend.member.repository.MemberRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Map<String, Object> signUp(SignupRequest request) {
        String email = request.normalizedEmail();
        if (memberRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered.");
        }

        Member member = new Member(
                email,
                passwordEncoder.encode(request.password()),
                request.normalizedNickname()
        );
        return toResponse(memberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> login(LoginRequest request) {
        String email = request.normalizedEmail();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        return toResponse(member);
    }

    private Map<String, Object> toResponse(Member member) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", member.getId());
        response.put("email", member.getEmail());
        response.put("nickname", member.getNickname());
        return response;
    }
}
