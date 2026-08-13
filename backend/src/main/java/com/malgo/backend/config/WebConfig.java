package com.malgo.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 프론트엔드와 백엔드가 서로 다른 주소/포트에서 실행될 때 API 요청을 허용하기 위한 CORS 설정
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/api/**")
                // 개발 중 프론트 주소
                .allowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5173"
                )

                // 허용할 HTTP 메서드
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )

                // 모든 요청 헤더 허용
                .allowedHeaders("*")

                // 쿠키나 인증 정보를 함께 보낼 수 있도록 허용
                .allowCredentials(true);
    }
}