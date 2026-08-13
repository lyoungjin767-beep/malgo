package com.malgo.backend.repository;

import com.malgo.backend.entity.AiPartner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// AI 대화 상대 데이터를 저장하고 조회하기 위한 Repository

public interface AiPartnerRepository
        extends JpaRepository<AiPartner, Long> {
    // 기본 AI(member_id = null) + 해당 회원이 만든 커스텀 AI 조회
    List<AiPartner> findByMemberIdOrMemberIsNull(Long memberId);
}