package com.malgo.backend.repository;

import com.malgo.backend.entity.CultureWarning;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CultureWarningRepository
        extends JpaRepository<CultureWarning, Long> {
}