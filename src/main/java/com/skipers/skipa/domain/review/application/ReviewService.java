package com.skipers.skipa.domain.review.application;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.application.ApprovedPatentValidator;
import com.skipers.skipa.domain.patent.application.PatentSortOption;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.domain.BusinessOpinion;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import com.skipers.skipa.domain.review.dto.request.BulkReviewCreateRequest;
import com.skipers.skipa.domain.review.dto.response.BulkReviewCreateResponse;
import com.skipers.skipa.domain.review.dto.response.ReviewConfirmResponse;
import com.skipers.skipa.domain.review.dto.response.ReviewResponse;
import com.skipers.skipa.domain.review.exception.ReviewException;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewCycleRepository reviewCycleRepository;
    private final PatentRepository patentRepository;
    private final ReportRepository reportRepository;
    private final ApprovedPatentValidator approvedPatentValidator;

    @Transactional
    public ReviewResponse create(Long patentId) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));
        approvedPatentValidator.validateApproved(patent);
        Department department = validateDepartment(patent);
        ReviewCycle reviewCycle = getActiveReviewCycle();
        LocalDate dueDate = requireDeadline(reviewCycle);

        Review existingReview = reviewRepository.findByReviewCycleIdAndPatentIdAndDepartmentId(
                reviewCycle.getId(),
                patentId,
                department.getId()
        ).orElse(null);
        if (existingReview != null && existingReview.getStatus() == ReviewStatus.SCHEDULED) {
            existingReview.request(findLatestReport(patent.getId()), dueDate);
            return ReviewResponse.from(existingReview);
        }
        if (existingReview != null) {
            throw new ReviewException(ErrorCode.DUPLICATE_REVIEW_REQUEST);
        }

        Review review = reviewRepository.save(createReview(
                patent,
                department,
                reviewCycle,
                findLatestReport(patent.getId()),
                dueDate
        ));

        return ReviewResponse.from(review);
    }

    @Transactional
    public BulkReviewCreateResponse createBulk(BulkReviewCreateRequest request) {
        ReviewCycle reviewCycle = getActiveReviewCycle();
        List<Long> patentIds = new ArrayList<>(new LinkedHashSet<>(request.patentIds()));
        LocalDate dueDate = requireDeadline(reviewCycle);
        Map<Long, Patent> patentsById = new HashMap<>();
        patentRepository.findAllById(patentIds).forEach(patent -> patentsById.put(patent.getId(), patent));

        Map<String, Review> existingReviewsByKey = new HashMap<>();
        if (!patentsById.isEmpty()) {
            reviewRepository.findAllByReviewCycleIdAndPatentIdIn(reviewCycle.getId(), patentsById.keySet())
                    .forEach(review -> existingReviewsByKey.put(
                            reviewKey(review.getPatent().getId(), review.getDepartment().getId()),
                            review
                    ));
        }

        List<Review> reviews = new ArrayList<>();
        List<BulkReviewCreateResponse.Item> items = new ArrayList<>();
        int requestedCount = 0;
        for (Long patentId : patentIds) {
            Patent patent = patentsById.get(patentId);
            if (patent == null) {
                items.add(BulkReviewCreateResponse.Item.skipped(patentId, ErrorCode.PATENT_NOT_FOUND.getCode()));
                continue;
            }
            try {
                approvedPatentValidator.validateApproved(patent);
            } catch (PatentException e) {
                items.add(BulkReviewCreateResponse.Item.skipped(patentId, e.getErrorCode().getCode()));
                continue;
            }

            Department department = patent.getCurrentDepartment();
            if (department == null) {
                items.add(BulkReviewCreateResponse.Item.skipped(
                        patentId,
                        ErrorCode.PATENT_DEPARTMENT_NOT_ASSIGNED.getCode()
                ));
                continue;
            }
            if (department.isInactive()) {
                items.add(BulkReviewCreateResponse.Item.skipped(patentId, ErrorCode.DEPARTMENT_INACTIVE.getCode()));
                continue;
            }
            Review existingReview = existingReviewsByKey.get(reviewKey(patentId, department.getId()));
            if (existingReview != null && existingReview.getStatus() == ReviewStatus.SCHEDULED) {
                existingReview.request(findLatestReport(patentId), dueDate);
                items.add(BulkReviewCreateResponse.Item.created(patentId));
                requestedCount++;
                continue;
            }
            if (existingReview != null) {
                items.add(BulkReviewCreateResponse.Item.skipped(patentId, ErrorCode.DUPLICATE_REVIEW_REQUEST.getCode()));
                continue;
            }

            Review review = createReview(
                    patent,
                    department,
                    reviewCycle,
                    findLatestReport(patentId),
                    dueDate
            );
            reviews.add(review);
            items.add(BulkReviewCreateResponse.Item.created(patentId));
            requestedCount++;
        }

        if (!reviews.isEmpty()) {
            reviewRepository.saveAll(reviews);
        }
        return new BulkReviewCreateResponse(
                reviewCycle.getId(),
                requestedCount,
                items.size() - requestedCount,
                items
        );
    }

    public Page<ReviewResponse> getAll(
            String status,
            String opinion,
            Long departmentId,
            Long patentId,
            Boolean checked,
            String sort,
            Pageable pageable
    ) {
        ReviewStatus parsedStatus = parseStatus(status);
        BusinessOpinion parsedOpinion = parseOpinion(opinion);
        ReviewCycle reviewCycle = getActiveReviewCycle();
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                PatentSortOption.parse(sort).reviewSort()
        );

        return reviewRepository.findAllByFilters(
                        reviewCycle.getId(),
                        parsedStatus,
                        parsedOpinion,
                        departmentId,
                        patentId,
                        checked,
                        sortedPageable
                )
                .map(ReviewResponse::from);
    }

    public ReviewResponse get(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));

        return ReviewResponse.from(review);
    }

    @Transactional
    public ReviewConfirmResponse confirm(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));
        if (review.getStatus() != ReviewStatus.SUBMITTED) {
            throw new ReviewException(ErrorCode.INVALID_REVIEW_STATUS);
        }

        review.confirm();
        return ReviewConfirmResponse.from(review);
    }

    private ReviewStatus parseStatus(String status) {
        if (status == null) {
            return null;
        }

        try {
            return ReviewStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new ReviewException(ErrorCode.INVALID_REQUEST);
        }
    }

    private BusinessOpinion parseOpinion(String opinion) {
        if (opinion == null) {
            return null;
        }

        try {
            return BusinessOpinion.valueOf(opinion);
        } catch (IllegalArgumentException e) {
            throw new ReviewException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Department validateDepartment(Patent patent) {
        Department department = patent.getCurrentDepartment();
        if (department == null) {
            throw new ReviewException(ErrorCode.PATENT_DEPARTMENT_NOT_ASSIGNED);
        }
        if (department.isInactive()) {
            throw new ReviewException(ErrorCode.DEPARTMENT_INACTIVE);
        }
        return department;
    }

    private ReviewCycle getActiveReviewCycle() {
        LocalDate today = LocalDate.now();
        return reviewCycleRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(today, today)
                .orElseThrow(() -> new ReviewException(ErrorCode.ACTIVE_REVIEW_CYCLE_NOT_FOUND));
    }

    private LocalDate requireDeadline(ReviewCycle reviewCycle) {
        if (reviewCycle.getDeadline() == null) {
            throw new ReviewException(ErrorCode.REVIEW_CYCLE_DEADLINE_NOT_SET);
        }
        return reviewCycle.getDeadline();
    }

    private Review createReview(
            Patent patent,
            Department department,
            ReviewCycle reviewCycle,
            Report report,
            LocalDate dueDate
    ) {
        return Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .report(report)
                .dueDate(dueDate)
                .build();
    }

    private Report findLatestReport(Long patentId) {
        return reportRepository.findFirstByPatentIdOrderByIdDesc(patentId).orElse(null);
    }

    private String reviewKey(Long patentId, Long departmentId) {
        return patentId + ":" + departmentId;
    }

}
