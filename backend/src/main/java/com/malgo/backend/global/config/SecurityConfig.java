package com.malgo.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    // 비밀번호 암호화
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        // React 프론트엔드 주소 허용
        configuration.setAllowedOrigins(
                List.of("http://localhost:3000")
        );

        // 사용할 HTTP Method 허용
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        // 모든 요청 헤더 허용
        configuration.setAllowedHeaders(
                List.of("*")
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        // 모든 API 경로에 CORS 설정 적용
        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    // Spring Security 설정
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // 위에서 만든 CORS 설정 사용
                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                // REST API이므로 CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        // CORS Preflight 요청 허용
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // 회원가입/로그인 API 허용
                        .requestMatchers(
                                "/api/v1/auth/**"
                        ).permitAll()

                        // 현재 개발 단계에서는 나머지 API도 허용
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}