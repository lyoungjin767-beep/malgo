package com.malgo.backend.customization.repository;

import com.malgo.backend.customization.entity.UserCustomization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCustomizationRepository extends JpaRepository<UserCustomization, Long> {

    Optional<UserCustomization> findByMemberId(Long memberId);
}
