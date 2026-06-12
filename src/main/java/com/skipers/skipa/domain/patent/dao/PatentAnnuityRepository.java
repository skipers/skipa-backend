package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.PatentAnnuity;
import com.skipers.skipa.domain.patent.domain.PatentAnnuityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatentAnnuityRepository extends JpaRepository<PatentAnnuity, Long> {

    Page<PatentAnnuity> findByPatentId(Long patentId, Pageable pageable);

    Page<PatentAnnuity> findByPatentIdAndStatus(Long patentId, PatentAnnuityStatus status, Pageable pageable);

    Optional<PatentAnnuity> findByIdAndPatentIdAndStatus(Long id, Long patentId, PatentAnnuityStatus status);

    List<PatentAnnuity> findByStatusAndDueDateBetween(PatentAnnuityStatus status, LocalDate startDate, LocalDate endDate);

    List<PatentAnnuity> findByStatusAndDueDateBefore(PatentAnnuityStatus status, LocalDate date);

    Optional<PatentAnnuity> findFirstByPatentIdAndStatusOrderByStartYearDescIdDesc(
            Long patentId,
            PatentAnnuityStatus status
    );

    boolean existsByPatentIdAndStartYear(Long patentId, Integer startYear);

    void deleteAllByPatentId(Long patentId);
}
