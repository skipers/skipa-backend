package com.skipers.skipa.domain.review.application;

import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewCycleType;
import com.skipers.skipa.domain.review.dto.request.ReviewCycleCreateRequest;
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
        validateDuplicateName(request.name());
        validateOverlappingPeriod(request.startDate(), request.endDate());

        ReviewCycle reviewCycle = reviewCycleRepository.save(ReviewCycle.builder()
                .name(request.name())
                .type(parseType(request.type()))
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build());

        return ReviewCycleResponse.from(reviewCycle);
    }

    public ReviewCycleResponse get(Long reviewCycleId) {
        return ReviewCycleResponse.from(getReviewCycle(reviewCycleId));
    }

    public Page<ReviewCycleResponse> getAll(Pageable pageable) {
        return reviewCycleRepository.findAllByOrderByStartDateDesc(pageable)
                .map(ReviewCycleResponse::from);
    }

    @Transactional
    public ReviewCycleResponse update(Long reviewCycleId, ReviewCycleUpdateRequest request) {
        ReviewCycle reviewCycle = getReviewCycle(reviewCycleId);

        validatePeriod(request.startDate(), request.endDate());
        validateDuplicateName(request.name(), reviewCycleId);
        validateOverlappingPeriod(request.startDate(), request.endDate(), reviewCycleId);

        reviewCycle.update(
                request.name(),
                parseType(request.type()),
                request.startDate(),
                request.endDate()
        );

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

    private ReviewCycleType parseType(String type) {
        try {
            return ReviewCycleType.valueOf(type);
        } catch (IllegalArgumentException exception) {
            throw new ReviewCycleException(ErrorCode.INVALID_REVIEW_CYCLE_TYPE);
        }
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ReviewCycleException(ErrorCode.INVALID_REVIEW_CYCLE_PERIOD);
        }
    }

    private void validateDuplicateName(String name) {
        if (reviewCycleRepository.existsByNameIgnoreCase(name)) {
            throw new ReviewCycleException(ErrorCode.DUPLICATE_REVIEW_CYCLE_NAME);
        }
    }

    private void validateDuplicateName(String name, Long reviewCycleId) {
        if (reviewCycleRepository.existsByNameIgnoreCaseAndIdNot(name, reviewCycleId)) {
            throw new ReviewCycleException(ErrorCode.DUPLICATE_REVIEW_CYCLE_NAME);
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
