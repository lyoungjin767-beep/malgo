package com.malgo.backend.repository;

import com.malgo.backend.entity.AiPartner;
import org.springframework.data.jpa.repository.JpaRepository;

// AI 대화 상대 데이터를 저장하고 조회하기 위한 Repository

public interface AiPartnerRepository
        extends JpaRepository<AiPartner, Long> {
}