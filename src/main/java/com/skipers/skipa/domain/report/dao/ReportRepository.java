package com.skipers.skipa.domain.report.dao;

import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.domain.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByPatentId(Long patentId, Pageable pageable);

    List<Report> findByPatentIdAndStatusOrderByIdDesc(Long patentId, ReportStatus status);

    List<Report> findByPatentIdAndStatusInOrderByIdDesc(Long patentId, Collection<ReportStatus> statuses);

    Optional<Report> findFirstByPatentIdOrderByIdDesc(Long patentId);

    Optional<Report> findFirstByPatentIdAndStatusOrderByIdDesc(Long patentId, ReportStatus status);

    Optional<Report> findFirstByPatentIdAndStatusInOrderByIdDesc(Long patentId, Collection<ReportStatus> statuses);

    List<Report> findAllByStatus(ReportStatus status);

    List<Report> findAllByStatusIn(Collection<ReportStatus> statuses);

    Optional<Report> findByIdAndPatentId(Long id, Long patentId);

    void deleteAllByPatentId(Long patentId);
}
