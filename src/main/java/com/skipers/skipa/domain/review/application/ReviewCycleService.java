package com.skipers.skipa.domain.review.application;

import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.dto.request.ReviewCycleCreateRequest;
import com.skipers.skipa.domain.review.dto.request.ReviewCycleDeadlineUpdateRequest;
import com.skipers.skipa.domain.review.dto.request.ReviewCycleUpdateRequest;
import com.skipers.skipa.domain.review.dto.response.ReviewCycleResponse;
import com.skipers.skipa.domain.review.exception.ReviewCycleException;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewCycleService {

    private final ReviewCycleRepository reviewCycleRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public ReviewCycleResponse create(ReviewCycleCreateRequest request) {
        validatePeriod(request.startDate(), request.endDate());
        validateDuplicateYearQuarter(request.year(), request.quarter());
        validateOverlappingPeriod(request.startDate(), request.endDate());

        ReviewCycle reviewCycle = reviewCycleRepository.save(ReviewCycle.builder()
                .year(request.year())
                .quarter(request.quarter())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build());

        return ReviewCycleResponse.from(reviewCycle);
    }

    public ReviewCycleResponse get(Long reviewCycleId) {
        return ReviewCycleResponse.from(getReviewCycle(reviewCycleId));
    }

    public ReviewCycleResponse getCurrent() {
        LocalDate today = LocalDate.now();
        ReviewCycle reviewCycle = reviewCycleRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(today, today)
                .orElseThrow(() -> new ReviewCycleException(ErrorCode.ACTIVE_REVIEW_CYCLE_NOT_FOUND));

        return ReviewCycleResponse.from(reviewCycle);
    }

    public Page<ReviewCycleResponse> getAll(Pageable pageable) {
        return reviewCycleRepository.findAllByOrderByStartDateDesc(pageable)
                .map(ReviewCycleResponse::from);
    }

    @Transactional
    public ReviewCycleResponse update(Long reviewCycleId, ReviewCycleUpdateRequest request) {
        ReviewCycle reviewCycle = getReviewCycle(reviewCycleId);

        validatePeriod(request.startDate(), request.endDate());
        validateDeadlineInPeriod(reviewCycle.getDeadline(), request.startDate(), request.endDate());
        validateDuplicateYearQuarter(request.year(), request.quarter(), reviewCycleId);
        validateOverlappingPeriod(request.startDate(), request.endDate(), reviewCycleId);

        reviewCycle.update(
                request.year(),
                request.quarter(),
                request.startDate(),
                request.endDate()
        );

        return ReviewCycleResponse.from(reviewCycle);
    }

    @Transactional
    public ReviewCycleResponse updateDeadline(Long reviewCycleId, ReviewCycleDeadlineUpdateRequest request) {
        ReviewCycle reviewCycle = getReviewCycle(reviewCycleId);
        validateDeadlineInPeriod(request.deadline(), reviewCycle.getStartDate(), reviewCycle.getEndDate());

        reviewCycle.updateDeadline(request.deadline());

        return ReviewCycleResponse.from(reviewCycle);
    }

    @Transactional
    public void delete(Long reviewCycleId) {
        ReviewCycle reviewCycle = getReviewCycle(reviewCycleId);

        if (reviewRepository.existsByReviewCycleId(reviewCycleId)) {
            throw new ReviewCycleException(ErrorCode.REVIEW_CYCLE_IN_USE);
        }

        reviewCycleRepository.delete(reviewCycle);
    }

    private ReviewCycle getReviewCycle(Long reviewCycleId) {
        return reviewCycleRepository.findById(reviewCycleId)
                .orElseThrow(() -> new ReviewCycleException(ErrorCode.REVIEW_CYCLE_NOT_FOUND));
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ReviewCycleException(ErrorCode.INVALID_REVIEW_CYCLE_PERIOD);
        }
    }

    private void validateDeadlineInPeriod(LocalDate deadline, LocalDate startDate, LocalDate endDate) {
        if (deadline == null) {
            return;
        }
        if (deadline.isBefore(startDate) || deadline.isAfter(endDate)) {
            throw new ReviewCycleException(ErrorCode.INVALID_REVIEW_CYCLE_DEADLINE);
        }
    }

    private void validateDuplicateYearQuarter(Integer year, Integer quarter) {
        if (reviewCycleRepository.existsByYearAndQuarter(year, quarter)) {
            throw new ReviewCycleException(ErrorCode.DUPLICATE_REVIEW_CYCLE);
        }
    }

    private void validateDuplicateYearQuarter(Integer year, Integer quarter, Long reviewCycleId) {
        if (reviewCycleRepository.existsByYearAndQuarterAndIdNot(year, quarter, reviewCycleId)) {
            throw new ReviewCycleException(ErrorCode.DUPLICATE_REVIEW_CYCLE);
        }
    }

    private void validateOverlappingPeriod(LocalDate startDate, LocalDate endDate) {
        if (reviewCycleRepository.existsByStartDateLessThanEqualAndEndDateGreaterThanEqual(endDate, startDate)) {
            throw new ReviewCycleException(ErrorCode.REVIEW_CYCLE_PERIOD_OVERLAP);
        }
    }

    private void validateOverlappingPeriod(LocalDate startDate, LocalDate endDate, Long reviewCycleId) {
        if (reviewCycleRepository.existsByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
                endDate,
                startDate,
                reviewCycleId
        )) {
            throw new ReviewCycleException(ErrorCode.REVIEW_CYCLE_PERIOD_OVERLAP);
        }
    }
}
