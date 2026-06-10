package com.skipers.skipa.domain.preevaluation.dao;

import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreEvaluationRepository extends JpaRepository<PreEvaluation, Long> {

    Page<PreEvaluation> findByUserId(Long userId, Pageable pageable);

    Optional<PreEvaluation> findByIdAndUserId(Long id, Long userId);
}
