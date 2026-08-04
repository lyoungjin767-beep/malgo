package com.malgo.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.malgo.backend.auth.controller.AuthController;
import com.malgo.backend.auth.dto.LoginRequest;
import com.malgo.backend.auth.dto.SignupRequest;
import com.malgo.backend.auth.entity.EmailVerification;
import com.malgo.backend.auth.entity.VerificationPurpose;
import com.malgo.backend.auth.repository.EmailVerificationRepository;
import com.malgo.backend.auth.service.AuthService;
import com.malgo.backend.auth.service.EmailVerificationService;
import com.malgo.backend.auth.service.MailService;
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
    private EmailVerificationService emailVerificationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = org.mockito.Mockito.mock(AuthService.class);
        emailVerificationService = org.mockito.Mockito.mock(EmailVerificationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, emailVerificationService)).build();
    }

    @Test
    void sendsSignupEmailCode() throws Exception {
        mockMvc.perform(post("/api/v1/auth/email/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "signup@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증번호를 전송했습니다."));

        org.mockito.Mockito.verify(emailVerificationService)
                .sendCode("signup@example.com", VerificationPurpose.SIGNUP);
    }

    @Test
    void verifiesSignupEmailCode() throws Exception {
        mockMvc.perform(post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "signup@example.com",
                                  "code": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 인증에 성공했습니다."));

        org.mockito.Mockito.verify(emailVerificationService)
                .verifyCode("signup@example.com", "123456", VerificationPurpose.SIGNUP);
    }

    @Test
    void signsUpMember() throws Exception {
        org.mockito.Mockito.when(authService.signup(org.mockito.ArgumentMatchers.any(SignupRequest.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "signup-user",
                                  "email": "signup@example.com",
                                  "password": "password123",
                                  "passwordConfirm": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("회원가입에 성공했습니다."))
                .andExpect(jsonPath("$.memberId").value(1));
    }

    @Test
    void logsInMember() throws Exception {
        org.mockito.Mockito.when(authService.login(org.mockito.ArgumentMatchers.any(LoginRequest.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.memberId").value(1));
    }

    @Test
    void storesHashedPassword() {
        MemberRepository memberRepository = org.mockito.Mockito.mock(MemberRepository.class);
        EmailVerificationService emailVerificationService = org.mockito.Mockito.mock(EmailVerificationService.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(memberRepository, passwordEncoder, emailVerificationService);

        org.mockito.Mockito.when(memberRepository.existsByEmail("hash@example.com")).thenReturn(false);
        org.mockito.Mockito.when(emailVerificationService.isVerified("hash@example.com", VerificationPurpose.SIGNUP))
                .thenReturn(true);
        org.mockito.Mockito.when(memberRepository.save(org.mockito.ArgumentMatchers.any(Member.class)))
                .thenAnswer(invocation -> {
                    Member member = invocation.getArgument(0);
                    ReflectionTestUtils.setField(member, "id", 1L);
                    return member;
                });

        Long memberId = service.signup(new SignupRequest("hash-user", "hash@example.com", "password123", "password123"));

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
        EmailVerificationService emailVerificationService = org.mockito.Mockito.mock(EmailVerificationService.class);
        AuthService service = new AuthService(memberRepository, new BCryptPasswordEncoder(), emailVerificationService);

        org.mockito.Mockito.when(memberRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
                service.signup(new SignupRequest("duplicate-user", "duplicate@example.com", "password123", "password123"))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsPasswordConfirmMismatchInService() {
        MemberRepository memberRepository = org.mockito.Mockito.mock(MemberRepository.class);
        EmailVerificationService emailVerificationService = org.mockito.Mockito.mock(EmailVerificationService.class);
        AuthService service = new AuthService(memberRepository, new BCryptPasswordEncoder(), emailVerificationService);

        assertThatThrownBy(() ->
                service.signup(new SignupRequest("mismatch-user", "mismatch@example.com", "password123", "different123"))
        ).isInstanceOf(IllegalArgumentException.class);

        org.mockito.Mockito.verifyNoInteractions(memberRepository);
        org.mockito.Mockito.verifyNoInteractions(emailVerificationService);
    }

    @Test
    void rejectsUnverifiedEmailInService() {
        MemberRepository memberRepository = org.mockito.Mockito.mock(MemberRepository.class);
        EmailVerificationService emailVerificationService = org.mockito.Mockito.mock(EmailVerificationService.class);
        AuthService service = new AuthService(memberRepository, new BCryptPasswordEncoder(), emailVerificationService);

        org.mockito.Mockito.when(memberRepository.existsByEmail("unverified@example.com")).thenReturn(false);
        org.mockito.Mockito.when(emailVerificationService.isVerified("unverified@example.com", VerificationPurpose.SIGNUP))
                .thenReturn(false);

        assertThatThrownBy(() ->
                service.signup(new SignupRequest("unverified-user", "unverified@example.com", "password123", "password123"))
        ).isInstanceOf(IllegalArgumentException.class);

        org.mockito.Mockito.verify(memberRepository, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any(Member.class));
    }

    @Test
    void emailVerificationServiceChecksLatestVerifiedEmail() {
        EmailVerificationRepository verificationRepository =
                org.mockito.Mockito.mock(EmailVerificationRepository.class);
        MailService mailService = org.mockito.Mockito.mock(MailService.class);
        EmailVerificationService emailVerificationService =
                new EmailVerificationService(verificationRepository, mailService);
        EmailVerification verification =
                new EmailVerification("verify@example.com", "123456", VerificationPurpose.SIGNUP);
        verification.verify();

        org.mockito.Mockito.when(verificationRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(
                        "verify@example.com",
                        VerificationPurpose.SIGNUP
                ))
                .thenReturn(Optional.of(verification));

        assertThat(emailVerificationService.isVerified("verify@example.com", VerificationPurpose.SIGNUP)).isTrue();
    }

    @Test
    void emailVerificationServiceVerifiesCode() {
        EmailVerificationRepository verificationRepository =
                org.mockito.Mockito.mock(EmailVerificationRepository.class);
        MailService mailService = org.mockito.Mockito.mock(MailService.class);
        EmailVerificationService emailVerificationService =
                new EmailVerificationService(verificationRepository, mailService);
        EmailVerification verification =
                new EmailVerification("verify@example.com", "123456", VerificationPurpose.SIGNUP);

        org.mockito.Mockito.when(verificationRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(
                        "verify@example.com",
                        VerificationPurpose.SIGNUP
                ))
                .thenReturn(Optional.of(verification));

        emailVerificationService.verifyCode("verify@example.com", "123456", VerificationPurpose.SIGNUP);

        assertThat(verification.isVerified()).isTrue();
    }

    @Test
    void logsInMemberInService() {
        MemberRepository memberRepository = org.mockito.Mockito.mock(MemberRepository.class);
        EmailVerificationService emailVerificationService = org.mockito.Mockito.mock(EmailVerificationService.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(memberRepository, passwordEncoder, emailVerificationService);
        Member member = new Member("login@example.com", passwordEncoder.encode("password123"), "login-user");
        ReflectionTestUtils.setField(member, "id", 1L);

        org.mockito.Mockito.when(memberRepository.findByEmail("login@example.com")).thenReturn(Optional.of(member));

        Long memberId = service.login(new LoginRequest("login@example.com", "password123"));

        assertThat(memberId).isEqualTo(1L);
    }

    @Test
    void rejectsInvalidPasswordInService() {
        MemberRepository memberRepository = org.mockito.Mockito.mock(MemberRepository.class);
        EmailVerificationService emailVerificationService = org.mockito.Mockito.mock(EmailVerificationService.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(memberRepository, passwordEncoder, emailVerificationService);
        Member member = new Member("login@example.com", passwordEncoder.encode("password123"), "login-user");

        org.mockito.Mockito.when(memberRepository.findByEmail("login@example.com")).thenReturn(Optional.of(member));

        assertThatThrownBy(() ->
                service.login(new LoginRequest("login@example.com", "wrong-password"))
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
