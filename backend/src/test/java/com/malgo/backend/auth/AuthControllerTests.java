package com.malgo.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

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
        org.mockito.Mockito.when(authService.signUp(org.mockito.ArgumentMatchers.any(SignUpRequest.class)))
                .thenReturn(new AuthResponse(1L, "signup@example.com", "signup-user"));

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
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("signup@example.com"))
                .andExpect(jsonPath("$.nickname").value("signup-user"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        org.mockito.Mockito.when(authService.signUp(org.mockito.ArgumentMatchers.any(SignUpRequest.class)))
                .thenReturn(new AuthResponse(1L, "duplicate@example.com", "duplicate-user"))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT));

        String payload = """
                {
                  "email": "duplicate@example.com",
                  "password": "password123",
                  "nickname": "duplicate-user"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void logsInMember() throws Exception {
        org.mockito.Mockito.when(authService.signUp(org.mockito.ArgumentMatchers.any(SignUpRequest.class)))
                .thenReturn(new AuthResponse(1L, "login@example.com", "login-user"));
        org.mockito.Mockito.when(authService.login(org.mockito.ArgumentMatchers.any(LoginRequest.class)))
                .thenReturn(new AuthResponse(1L, "login@example.com", "login-user"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "password123",
                                  "nickname": "login-user"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(jsonPath("$.nickname").value("login-user"));
    }

    @Test
    void rejectsInvalidLogin() throws Exception {
        org.mockito.Mockito.when(authService.login(org.mockito.ArgumentMatchers.any(LoginRequest.class)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void storesHashedPassword() throws Exception {
        MemberRepository memberRepository = org.mockito.Mockito.mock(MemberRepository.class);
        AuthService service = new AuthService(memberRepository, new BCryptPasswordEncoder());

        org.mockito.Mockito.when(memberRepository.existsByEmail("hash@example.com")).thenReturn(false);
        org.mockito.Mockito.when(memberRepository.save(org.mockito.ArgumentMatchers.any(Member.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.signUp(new SignUpRequest("hash@example.com", "password123", "hash-user"));

        org.mockito.ArgumentCaptor<Member> captor = org.mockito.ArgumentCaptor.forClass(Member.class);
        org.mockito.Mockito.verify(memberRepository).save(captor.capture());

        Member member = captor.getValue();
        org.hamcrest.MatcherAssert.assertThat(member.getPassword(), not("password123"));
    }

    @Test
    void rejectsDuplicateEmailInService() {
        MemberRepository memberRepository = org.mockito.Mockito.mock(MemberRepository.class);
        AuthService service = new AuthService(memberRepository, new BCryptPasswordEncoder());

        org.mockito.Mockito.when(memberRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        ResponseStatusException exception = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> service.signUp(new SignUpRequest("duplicate@example.com", "password123", "duplicate-user"))
        );

        assertThat(exception.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void logsInMemberInService() {
        MemberRepository memberRepository = org.mockito.Mockito.mock(MemberRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(memberRepository, passwordEncoder);
        Member member = Member.create("login@example.com", passwordEncoder.encode("password123"), "login-user");

        org.mockito.Mockito.when(memberRepository.findByEmail("login@example.com")).thenReturn(Optional.of(member));

        AuthResponse response = service.login(new LoginRequest("login@example.com", "password123"));

        assertThat(response.email()).isEqualTo("login@example.com");
        assertThat(response.nickname()).isEqualTo("login-user");
    }
}
