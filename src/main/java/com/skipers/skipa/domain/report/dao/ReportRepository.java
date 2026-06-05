package com.skipers.skipa.domain.report.dao;

import com.skipers.skipa.domain.report.domain.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByPatentId(Long patentId, Pageable pageable);

    Optional<Report> findFirstByPatentIdOrderByIdDesc(Long patentId);

    Optional<Report> findByIdAndPatentId(Long id, Long patentId);

    void deleteAllByPatentId(Long patentId);
}
