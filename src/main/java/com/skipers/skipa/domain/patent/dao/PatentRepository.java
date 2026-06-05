package com.skipers.skipa.domain.patent.dao;

import com.skipers.skipa.domain.patent.domain.Patent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select p.techField
            from Patent p
            where p.techField is not null
              and trim(p.techField) <> ''
            """)
    List<String> findAllTechFields();

    @Query("""
            select p.expiryDate
            from Patent p
            where p.expiryDate is not null
              and p.expiryDate >= :expiryDate
            """)
    List<LocalDate> findAllExpiryDatesFrom(@Param("expiryDate") LocalDate expiryDate);
}
