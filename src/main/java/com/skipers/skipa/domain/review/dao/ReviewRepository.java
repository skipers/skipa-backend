package com.skipers.skipa.domain.review.dao;

import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByDepartmentId(Long departmentId, Pageable pageable);

    @Query("""
            select review
            from Review review
            where (:status is null or review.status = :status)
              and (:departmentId is null or review.department.id = :departmentId)
              and (:patentId is null or review.patent.id = :patentId)
            """)
    Page<Review> findAllByFilters(
            @Param("status") ReviewStatus status,
            @Param("departmentId") Long departmentId,
            @Param("patentId") Long patentId,
            Pageable pageable
    );

    Optional<Review> findByPatentIdAndDepartmentId(Long patentId, Long departmentId);

    boolean existsByPatentIdAndDepartmentId(Long patentId, Long departmentId);

    boolean existsByPatentId(Long patentId);

    void deleteAllByPatentId(Long patentId);
}
