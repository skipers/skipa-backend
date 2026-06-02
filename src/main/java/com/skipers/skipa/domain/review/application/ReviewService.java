package com.skipers.skipa.domain.review.application;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import com.skipers.skipa.domain.review.dto.response.ReviewResponse;
import com.skipers.skipa.domain.review.exception.ReviewException;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewCycleRepository reviewCycleRepository;
    private final PatentRepository patentRepository;

    @Transactional
    public ReviewResponse create(Long patentId) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));
        Department department = patent.getCurrentDepartment();
        if (department == null) {
            throw new ReviewException(ErrorCode.PATENT_DEPARTMENT_NOT_ASSIGNED);
        }
        if (department.isInactive()) {
            throw new ReviewException(ErrorCode.DEPARTMENT_INACTIVE);
        }
        LocalDate today = LocalDate.now();
        ReviewCycle reviewCycle = reviewCycleRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(today, today)
                .orElseThrow(() -> new ReviewException(ErrorCode.ACTIVE_REVIEW_CYCLE_NOT_FOUND));

        if (reviewRepository.existsByReviewCycleIdAndPatentIdAndDepartmentId(
                reviewCycle.getId(),
                patentId,
                department.getId()
        )) {
            throw new ReviewException(ErrorCode.DUPLICATE_REVIEW_REQUEST);
        }

        Review review = reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .build());

        return ReviewResponse.from(review);
    }

    public Page<ReviewResponse> getAll(String status, Long departmentId, Long patentId, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        return reviewRepository.findAllByFilters(parseStatus(status), departmentId, patentId, sortedPageable)
                .map(ReviewResponse::from);
    }

    public ReviewResponse get(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));

        return ReviewResponse.from(review);
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
}
