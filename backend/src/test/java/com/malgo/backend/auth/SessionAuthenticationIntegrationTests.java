package com.malgo.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.malgo.backend.member.entity.Member;
import com.malgo.backend.member.repository.MemberRepository;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class SessionAuthenticationIntegrationTests {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member member;
    private Member otherMember;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        member = memberRepository.save(
                new Member(
                        "session-" + suffix,
                        passwordEncoder.encode(PASSWORD)
                )
        );
        otherMember = memberRepository.save(
                new Member(
                        "other-" + suffix,
                        passwordEncoder.encode(PASSWORD)
                )
        );
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteById(member.getId());
        memberRepository.deleteById(otherMember.getId());
    }

    @Test
    void rejectsProtectedRequestWithoutSession() throws Exception {
        mockMvc.perform(get(
                        "/api/partners/member/{memberId}",
                        member.getId()
                ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsSessionAndUsesItForProtectedRequest() throws Exception {
        MockHttpSession session = login();

        SecurityContext securityContext = (SecurityContext) session
                .getAttribute(
                        HttpSessionSecurityContextRepository
                                .SPRING_SECURITY_CONTEXT_KEY
                );

        assertThat(securityContext).isNotNull();
        assertThat(securityContext.getAuthentication().isAuthenticated())
                .isTrue();
        assertThat(securityContext.getAuthentication().getName())
                .isEqualTo(member.getUsername());

        mockMvc.perform(get(
                        "/api/partners/member/{memberId}",
                        member.getId()
                ).session(session))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsDifferentMemberIdWithAuthenticatedSession() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(get(
                        "/api/partners/member/{memberId}",
                        otherMember.getId()
                ).session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "로그인한 회원과 요청 회원이 일치하지 않습니다."
                ));
    }

    @Test
    void rejectsDifferentMemberIdInRequestBody() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(post("/api/conversations")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": %d,
                                  "aiPartnerId": null,
                                  "situation": "BUSINESS",
                                  "field": "IT_DEVELOPMENT",
                                  "targetCountry": "US"
                                }
                                """.formatted(otherMember.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "로그인한 회원과 요청 회원이 일치하지 않습니다."
                ));
    }

    @Test
    void rejectsInvalidPasswordWithoutCreatingAuthenticatedSession()
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(
                        "아이디 또는 비밀번호가 올바르지 않습니다."
                ))
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);

        if (session != null) {
            assertThat(session.getAttribute(
                    HttpSessionSecurityContextRepository
                            .SPRING_SECURITY_CONTEXT_KEY
            )).isNull();
        }
    }

    @Test
    void invalidatesSessionOnLogout() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(post("/api/v1/auth/logout").session(session))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();

        mockMvc.perform(get(
                        "/api/partners/member/{memberId}",
                        member.getId()
                ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsCredentialedCorsRequestsFromFrontendOrigin()
            throws Exception {
        mockMvc.perform(options(
                        "/api/partners/member/{memberId}",
                        member.getId()
                )
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                "GET"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:3000"
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                        "true"
                ));
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().string(member.getId().toString()))
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        assertThat(session).isInstanceOf(MockHttpSession.class);

        return (MockHttpSession) session;
    }

    private String loginBody(String password) {
        return """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(member.getUsername(), password);
    }
}
