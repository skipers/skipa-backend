package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.Patent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatentRepository extends JpaRepository<Patent, Long> {

    boolean existsByApplicationNumber(String applicationNumber);

    Optional<Patent> findByApplicationNumber(String applicationNumber);

    Page<Patent> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Patent> findByCurrentDepartmentId(Long departmentId, Pageable pageable);

    Page<Patent> findByCurrentDepartmentIdAndTitleContainingIgnoreCase(Long departmentId, String keyword, Pageable pageable);

    long countByCurrentDepartmentIsNull();

    long countByCurrentDepartmentIsNotNull();

    List<Patent> findByTechFieldIsNotNull();

    List<Patent> findByExpiryDateIsNotNullAndExpiryDateGreaterThanEqual(LocalDate expiryDate);
}
