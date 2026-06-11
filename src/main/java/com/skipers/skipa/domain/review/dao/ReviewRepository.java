package com.skipers.skipa.domain.review.dao;

import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.BusinessOpinion;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.time.Instant;
import java.time.LocalDate;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = "patent")
    @Query("""
            select review
            from Review review
            where review.department.id = :departmentId
              and review.reviewCycle.id = :reviewCycleId
              and review.patent.currentDepartment.id = :departmentId
              and review.status <> com.skipers.skipa.domain.review.domain.ReviewStatus.SCHEDULED
              and (:status is null or review.status = :status)
              and (:opinion is null or review.opinion = :opinion)
              and (:submittedFromProvided = false or review.submittedAt >= :submittedFrom)
              and (:submittedToProvided = false or review.submittedAt < :submittedTo)
              and review.id = (
                  select max(latestReview.id)
                  from Review latestReview
                  where latestReview.patent.id = review.patent.id
                    and latestReview.department.id = :departmentId
                    and latestReview.reviewCycle.id = :reviewCycleId
                    and latestReview.status <> com.skipers.skipa.domain.review.domain.ReviewStatus.SCHEDULED
              )
            """)
    Page<Review> findLatestBusinessReviewsByReviewCycleIdAndDepartmentId(
            @Param("reviewCycleId") Long reviewCycleId,
            @Param("departmentId") Long departmentId,
            @Param("status") ReviewStatus status,
            @Param("opinion") BusinessOpinion opinion,
            @Param("submittedFromProvided") boolean submittedFromProvided,
            @Param("submittedFrom") Instant submittedFrom,
            @Param("submittedToProvided") boolean submittedToProvided,
            @Param("submittedTo") Instant submittedTo,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"patent", "department", "reviewCycle", "report"})
    @Query("""
            select review
            from Review review
            where review.department.id = :departmentId
              and review.status = com.skipers.skipa.domain.review.domain.ReviewStatus.SUBMITTED
              and review.reviewCycle.endDate < :today
              and (:year is null or review.reviewCycle.year = :year)
              and (:quarter is null or review.reviewCycle.quarter = :quarter)
              and (:opinion is null or review.opinion = :opinion)
            order by review.reviewCycle.year desc,
                     review.reviewCycle.quarter desc,
                     review.submittedAt desc,
                     review.id desc
            """)
    Page<Review> findSubmittedBusinessReviewHistory(
            @Param("departmentId") Long departmentId,
            @Param("today") LocalDate today,
            @Param("year") Integer year,
            @Param("quarter") Integer quarter,
            @Param("opinion") BusinessOpinion opinion,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"patent", "department", "reviewCycle", "report"})
    @Query("""
            select review
            from Review review
            where review.reviewCycle.id = :reviewCycleId
              and (:status is null or review.status = :status)
              and (:departmentId is null or review.department.id = :departmentId)
              and (:patentId is null or review.patent.id = :patentId)
              and (:checked is null or review.checked = :checked)
            """)
    Page<Review> findAllByFilters(
            @Param("reviewCycleId") Long reviewCycleId,
            @Param("status") ReviewStatus status,
            @Param("departmentId") Long departmentId,
            @Param("patentId") Long patentId,
            @Param("checked") Boolean checked,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"patent", "department", "report"})
    Optional<Review> findFirstByPatentIdAndDepartmentIdOrderByIdDesc(Long patentId, Long departmentId);

    @EntityGraph(attributePaths = {"patent", "department", "reviewCycle", "report"})
    Optional<Review> findFirstByReviewCycleIdAndPatentIdAndDepartmentIdAndStatusInOrderByIdDesc(
            Long reviewCycleId,
            Long patentId,
            Long departmentId,
            Collection<ReviewStatus> statuses
    );

    @EntityGraph(attributePaths = {"report"})
    List<Review> findByPatentIdAndReportIdInAndStatus(
            Long patentId,
            Collection<Long> reportIds,
            ReviewStatus status
    );

    Optional<Review> findFirstByPatentIdAndReportIdAndStatusOrderByIdDesc(
            Long patentId,
            Long reportId,
            ReviewStatus status
    );

    @Override
    @EntityGraph(attributePaths = {"patent", "department", "reviewCycle", "report"})
    Optional<Review> findById(Long id);

    Optional<Review> findByReviewCycleIdAndPatentIdAndDepartmentId(Long reviewCycleId, Long patentId, Long departmentId);

    boolean existsByReviewCycleIdAndPatentIdAndDepartmentId(Long reviewCycleId, Long patentId, Long departmentId);

    List<Review> findAllByReviewCycleIdAndPatentIdIn(Long reviewCycleId, Collection<Long> patentIds);

    @EntityGraph(attributePaths = {"patent", "department", "reviewCycle"})
    List<Review> findAllByReviewCycleId(Long reviewCycleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Review review
            set review.status = com.skipers.skipa.domain.review.domain.ReviewStatus.OVERDUE
            where review.status in :statuses
              and review.dueDate < :today
            """)
    int markOverdueByDueDateBefore(
            @Param("today") LocalDate today,
            @Param("statuses") Collection<ReviewStatus> statuses
    );

    boolean existsByReviewCycleId(Long reviewCycleId);

    void deleteAllByPatentId(Long patentId);
}
