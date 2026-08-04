package com.malgo.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.malgo.backend.auth.controller.AuthController;
import com.malgo.backend.auth.dto.LoginRequest;
import com.malgo.backend.auth.dto.SignupRequest;
import com.malgo.backend.auth.service.AuthService;
import com.malgo.backend.member.entity.Member;
import com.malgo.backend.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTests {

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = org.mockito.Mockito.mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService)).build();
    }

    @Test
    void signsUpMember() throws Exception {
        org.mockito.Mockito.when(authService.signup(org.mockito.ArgumentMatchers.any(SignupRequest.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "signup@example.com",
                                  "password": "password123",
                                  "nickname": "signup-user"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(1));
    }

    @Test
    void logsInMember() throws Exception {
        org.mockito.Mockito.when(authService.login(org.mockito.ArgumentMatchers.any(LoginRequest.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));
    }

    @Test
    void storesHashedPassword() {
        MemberRepository memberRepository = org.mockito.Mockito.mock(MemberRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(memberRepository, passwordEncoder);

        org.mockito.Mockito.when(memberRepository.existsByEmail("hash@example.com")).thenReturn(false);
        org.mockito.Mockito.when(memberRepository.save(org.mockito.ArgumentMatchers.any(Member.class)))
                .thenAnswer(invocation -> {
                    Member member = invocation.getArgument(0);
                    ReflectionTestUtils.setField(member, "id", 1L);
                    return member;
                });

        Long memberId = service.signup(new SignupRequest("hash@example.com", "password123", "hash-user"));

        org.mockito.ArgumentCaptor<Member> captor = org.mockito.ArgumentCaptor.forClass(Member.class);
        org.mockito.Mockito.verify(memberRepository).save(captor.capture());

        Member member = captor.getValue();
        assertThat(memberId).isEqualTo(1L);
        assertThat(member.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", member.getPassword())).isTrue();
    }

    @Test
    void rejectsDuplicateEmailInService() {
        MemberRepository memberRepository = org.mockito.Mockito.mock(MemberRepository.class);
        AuthService service = new AuthService(memberRepository, new BCryptPasswordEncoder());

        org.mockito.Mockito.when(memberRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
                service.signup(new SignupRequest("duplicate@example.com", "password123", "duplicate-user"))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void logsInMemberInService() {
        MemberRepository memberRepository = org.mockito.Mockito.mock(MemberRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(memberRepository, passwordEncoder);
        Member member = new Member("login@example.com", passwordEncoder.encode("password123"), "login-user");
        ReflectionTestUtils.setField(member, "id", 1L);

        org.mockito.Mockito.when(memberRepository.findByEmail("login@example.com")).thenReturn(Optional.of(member));

        Long memberId = service.login(new LoginRequest("login@example.com", "password123"));

        assertThat(memberId).isEqualTo(1L);
    }

    @Test
    void rejectsInvalidPasswordInService() {
        MemberRepository memberRepository = org.mockito.Mockito.mock(MemberRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(memberRepository, passwordEncoder);
        Member member = new Member("login@example.com", passwordEncoder.encode("password123"), "login-user");

        org.mockito.Mockito.when(memberRepository.findByEmail("login@example.com")).thenReturn(Optional.of(member));

        assertThatThrownBy(() ->
                service.login(new LoginRequest("login@example.com", "wrong-password"))
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
