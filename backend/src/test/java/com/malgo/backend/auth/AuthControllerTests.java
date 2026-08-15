package com.malgo.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
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
        authService = Mockito.mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AuthController(authService)
        ).build();
    }

    @Test
    void signsUpMember() throws Exception {
        Mockito.when(authService.signup(ArgumentMatchers.any(SignupRequest.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "signup-user",
                                  "password": "password123",
                                  "passwordConfirm": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string("1"));
    }

    @Test
    void logsInMember() throws Exception {
        Mockito.when(authService.login(ArgumentMatchers.any(LoginRequest.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "login-user",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    void storesHashedPassword() {
        MemberRepository memberRepository = Mockito.mock(MemberRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(memberRepository, passwordEncoder);

        Mockito.when(memberRepository.existsByUsername("hash-user")).thenReturn(false);
        Mockito.when(memberRepository.save(ArgumentMatchers.any(Member.class)))
                .thenAnswer(invocation -> {
                    Member member = invocation.getArgument(0);
                    ReflectionTestUtils.setField(member, "id", 1L);
                    return member;
                });

        Long memberId = service.signup(new SignupRequest(
                "hash-user",
                "password123",
                "password123"
        ));

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        Mockito.verify(memberRepository).save(captor.capture());

        Member member = captor.getValue();
        assertThat(memberId).isEqualTo(1L);
        assertThat(member.getUsername()).isEqualTo("hash-user");
        assertThat(member.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", member.getPassword())).isTrue();
    }

    @Test
    void rejectsDuplicateUsernameInService() {
        MemberRepository memberRepository = Mockito.mock(MemberRepository.class);
        AuthService service = new AuthService(
                memberRepository,
                new BCryptPasswordEncoder()
        );

        Mockito.when(memberRepository.existsByUsername("duplicate-user"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                service.signup(new SignupRequest(
                        "duplicate-user",
                        "password123",
                        "password123"
                ))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsPasswordConfirmMismatchInService() {
        MemberRepository memberRepository = Mockito.mock(MemberRepository.class);
        AuthService service = new AuthService(
                memberRepository,
                new BCryptPasswordEncoder()
        );

        assertThatThrownBy(() ->
                service.signup(new SignupRequest(
                        "mismatch-user",
                        "password123",
                        "different123"
                ))
        ).isInstanceOf(IllegalArgumentException.class);

        Mockito.verifyNoInteractions(memberRepository);
    }

    @Test
    void logsInMemberInService() {
        MemberRepository memberRepository = Mockito.mock(MemberRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(memberRepository, passwordEncoder);
        Member member = new Member(
                "login-user",
                passwordEncoder.encode("password123")
        );
        ReflectionTestUtils.setField(member, "id", 1L);

        Mockito.when(memberRepository.findByUsername("login-user"))
                .thenReturn(Optional.of(member));

        Long memberId = service.login(new LoginRequest(
                "login-user",
                "password123"
        ));

        assertThat(memberId).isEqualTo(1L);
    }

    @Test
    void rejectsInvalidPasswordInService() {
        MemberRepository memberRepository = Mockito.mock(MemberRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(memberRepository, passwordEncoder);
        Member member = new Member(
                "login-user",
                passwordEncoder.encode("password123")
        );

        Mockito.when(memberRepository.findByUsername("login-user"))
                .thenReturn(Optional.of(member));

        assertThatThrownBy(() ->
                service.login(new LoginRequest(
                        "login-user",
                        "wrong-password"
                ))
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
