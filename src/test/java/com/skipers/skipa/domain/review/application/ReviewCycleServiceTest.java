package com.skipers.skipa.domain.review.application;

import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.dto.request.ReviewCycleCreateRequest;
import com.skipers.skipa.domain.review.dto.request.ReviewCycleUpdateRequest;
import com.skipers.skipa.domain.review.dto.response.ReviewCycleResponse;
import com.skipers.skipa.domain.review.exception.ReviewCycleException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewCycleServiceTest {

    @Mock
    private ReviewCycleRepository reviewCycleRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewCycleService reviewCycleService;

    private ReviewCycle reviewCycle;

    @BeforeEach
    void setUp() {
        reviewCycle = ReviewCycle.builder()
                .year(2026)
                .quarter(2)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();
        ReflectionTestUtils.setField(reviewCycle, "id", 1L);
    }

    @Test
    void createSavesReviewCycle() {
        ReviewCycleCreateRequest request = createRequest();
        when(reviewCycleRepository.save(any(ReviewCycle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewCycleResponse response = reviewCycleService.create(request);

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.quarter()).isEqualTo(3);
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void createRejectsInvalidPeriodDuplicateYearQuarterAndOverlap() {
        assertReviewCycleError(
                () -> reviewCycleService.create(new ReviewCycleCreateRequest(
                        2026,
                        3,
                        LocalDate.of(2026, 9, 30),
                        LocalDate.of(2026, 7, 1)
                )),
                ErrorCode.INVALID_REVIEW_CYCLE_PERIOD
        );

        ReviewCycleCreateRequest request = createRequest();
        when(reviewCycleRepository.existsByYearAndQuarter(request.year(), request.quarter())).thenReturn(true);
        assertReviewCycleError(
                () -> reviewCycleService.create(request),
                ErrorCode.DUPLICATE_REVIEW_CYCLE
        );

        when(reviewCycleRepository.existsByYearAndQuarter(request.year(), request.quarter())).thenReturn(false);
        when(reviewCycleRepository.existsByStartDateLessThanEqualAndEndDateGreaterThanEqual(
                request.endDate(),
                request.startDate()
        )).thenReturn(true);
        assertReviewCycleError(
                () -> reviewCycleService.create(request),
                ErrorCode.REVIEW_CYCLE_PERIOD_OVERLAP
        );
    }

    @Test
    void getAndGetAllReturnReviewCycles() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(reviewCycleRepository.findById(1L)).thenReturn(Optional.of(reviewCycle));
        when(reviewCycleRepository.findAllByOrderByStartDateDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(reviewCycle), pageable, 1));

        assertThat(reviewCycleService.get(1L).year()).isEqualTo(2026);
        assertThat(reviewCycleService.getAll(pageable).getContent())
                .extracting(ReviewCycleResponse::id)
                .containsExactly(1L);
    }

    @Test
    void getCurrentReturnsActiveReviewCycleNameAndRange() {
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(Optional.of(reviewCycle));

        ReviewCycleResponse response = reviewCycleService.getCurrent();

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.quarter()).isEqualTo(2);
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void getCurrentRejectsMissingActiveReviewCycle() {
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(Optional.empty());

        assertReviewCycleError(
                () -> reviewCycleService.getCurrent(),
                ErrorCode.ACTIVE_REVIEW_CYCLE_NOT_FOUND
        );
    }

    @Test
    void updateChangesReviewCycle() {
        ReviewCycleUpdateRequest request = new ReviewCycleUpdateRequest(
                2026,
                3,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 30)
        );
        when(reviewCycleRepository.findById(1L)).thenReturn(Optional.of(reviewCycle));

        ReviewCycleResponse response = reviewCycleService.update(1L, request);

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.quarter()).isEqualTo(3);
        assertThat(reviewCycle.getStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    void deleteRemovesUnusedReviewCycleAndRejectsUsedCycle() {
        when(reviewCycleRepository.findById(1L)).thenReturn(Optional.of(reviewCycle));

        reviewCycleService.delete(1L);

        verify(reviewCycleRepository).delete(reviewCycle);

        when(reviewRepository.existsByReviewCycleId(1L)).thenReturn(true);
        assertReviewCycleError(
                () -> reviewCycleService.delete(1L),
                ErrorCode.REVIEW_CYCLE_IN_USE
        );
    }

    @Test
    void getRejectsMissingReviewCycle() {
        when(reviewCycleRepository.findById(99L)).thenReturn(Optional.empty());

        assertReviewCycleError(
                () -> reviewCycleService.get(99L),
                ErrorCode.REVIEW_CYCLE_NOT_FOUND
        );
        verify(reviewCycleRepository, never()).delete(any());
    }

    private ReviewCycleCreateRequest createRequest() {
        return new ReviewCycleCreateRequest(
                2026,
                3,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 30)
        );
    }

    private void assertReviewCycleError(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(ReviewCycleException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
