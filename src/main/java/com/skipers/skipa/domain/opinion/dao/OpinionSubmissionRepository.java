package com.skipers.skipa.domain.opinion.dao;

import com.skipers.skipa.domain.opinion.domain.OpinionSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OpinionSubmissionRepository extends JpaRepository<OpinionSubmission, Long> {

    Page<OpinionSubmission> findByDepartmentId(Long departmentId, Pageable pageable);

    Optional<OpinionSubmission> findByPatentIdAndDepartmentId(Long patentId, Long departmentId);

    boolean existsByPatentId(Long patentId);

    void deleteAllByPatentId(Long patentId);
}
