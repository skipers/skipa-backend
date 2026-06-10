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

    @Query("""
            select patent
            from Patent patent
            where lower(patent.title) like lower(concat('%', :keyword, '%'))
               or lower(patent.applicationNumber) like lower(concat('%', :keyword, '%'))
               or lower(patent.inventor) like lower(concat('%', :keyword, '%'))
               or lower(patent.applicant) like lower(concat('%', :keyword, '%'))
            """)
    Page<Patent> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Page<Patent> findByCurrentDepartmentId(Long departmentId, Pageable pageable);

    List<Patent> findByExpiryDateBefore(LocalDate date);

    @Query("""
            select patent
            from Patent patent
            where patent.currentDepartment.id = :departmentId
              and (
                    lower(patent.title) like lower(concat('%', :keyword, '%'))
                 or lower(patent.applicationNumber) like lower(concat('%', :keyword, '%'))
                 or lower(patent.inventor) like lower(concat('%', :keyword, '%'))
                 or lower(patent.applicant) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<Patent> searchByCurrentDepartmentIdAndKeyword(
            @Param("departmentId") Long departmentId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    long countByCurrentDepartmentIsNull();
}
