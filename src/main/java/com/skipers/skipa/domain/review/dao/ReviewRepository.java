package com.skipers.skipa.domain.review.dao;

import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = "patent")
    @Query("""
            select review
            from Review review
            where review.department.id = :departmentId
              and review.patent.currentDepartment.id = :departmentId
              and review.id = (
                  select max(latestReview.id)
                  from Review latestReview
                  where latestReview.patent.id = review.patent.id
                    and latestReview.department.id = :departmentId
              )
            """)
    Page<Review> findLatestBusinessReviewsByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

    @EntityGraph(attributePaths = {"patent", "department", "reviewCycle", "report"})
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

    @EntityGraph(attributePaths = {"patent", "department", "report"})
    Optional<Review> findFirstByPatentIdAndDepartmentIdOrderByIdDesc(Long patentId, Long departmentId);

    @Override
    @EntityGraph(attributePaths = {"patent", "department", "reviewCycle", "report"})
    Optional<Review> findById(Long id);

    boolean existsByReviewCycleIdAndPatentIdAndDepartmentId(Long reviewCycleId, Long patentId, Long departmentId);

    List<Review> findAllByReviewCycleIdAndPatentIdIn(Long reviewCycleId, Collection<Long> patentIds);

    boolean existsByReviewCycleId(Long reviewCycleId);

    long countByReviewCycleId(Long reviewCycleId);

    long countByReviewCycleIdAndStatus(Long reviewCycleId, ReviewStatus status);

    long countByReviewCycleIdAndStatusAndDueDateBefore(Long reviewCycleId, ReviewStatus status, LocalDate dueDate);

    @EntityGraph(attributePaths = {"department"})
    List<Review> findAllByReviewCycleId(Long reviewCycleId);

    void deleteAllByPatentId(Long patentId);
}
