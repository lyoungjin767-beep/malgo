package com.malgo.backend.config;

import com.malgo.backend.entity.AiPartner;
import com.malgo.backend.repository.AiPartnerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 개발 중 사용할 기본 AI 대화 상대 데이터를 등록
// DB에 AI 상대가 하나도 없을 때만 Tom, kash, sana 기본 데이터를 생성

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initAiPartners(
            AiPartnerRepository aiPartnerRepository
    ) {
        return args -> {

            // 이미 데이터가 있다면 중복 생성하지 않음
            if (aiPartnerRepository.count() > 0) {
                return;
            }

            // 기본 AI 상대 - Tom
            aiPartnerRepository.save(
                    new AiPartner(
                            "Tom",
                            "US",
                            "EN",
                            "CLIENT",
                            "WORKER",
                            "MALE",
                            "POLITE",
                            null,
                            false
                    )
            );

            // 기본 AI 상대 - kash
            aiPartnerRepository.save(
                    new AiPartner(
                            "kash",
                            "JP",
                            "JA",
                            "FRIEND",
                            "COLLEGE_STUDENT",
                            "MALE",
                            "FRIENDLY",
                            null,
                            false
                    )
            );

            // 기본 AI 상대 - sana
            aiPartnerRepository.save(
                    new AiPartner(
                            "sana",
                            "VN",
                            "VI",
                            "BOSS",
                            "WORKER",
                            "FEMALE",
                            "POLITE",
                            null,
                            false
                    )
            );
        };
    }
}
