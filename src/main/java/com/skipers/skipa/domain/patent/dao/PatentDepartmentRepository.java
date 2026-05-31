package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.PatentDepartment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatentDepartmentRepository extends JpaRepository<PatentDepartment, Long> {

    Page<PatentDepartment> findByPatentId(Long patentId, Pageable pageable);

    Optional<PatentDepartment> findFirstByPatentIdOrderByAssignedAtDesc(Long patentId);

    void deleteAllByPatentId(Long patentId);
}
