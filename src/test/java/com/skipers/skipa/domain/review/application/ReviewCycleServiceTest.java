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
                .name("2026년 2분기 정기 재평가")
                .type(ReviewCycleType.QUARTERLY)
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

        assertThat(response.name()).isEqualTo("2026년 3분기 정기 재평가");
        assertThat(response.type()).isEqualTo("QUARTERLY");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void createRejectsInvalidPeriodDuplicateNameOverlapAndInvalidType() {
        assertReviewCycleError(
                () -> reviewCycleService.create(new ReviewCycleCreateRequest(
                        "잘못된 주기",
                        "QUARTERLY",
                        LocalDate.of(2026, 9, 30),
                        LocalDate.of(2026, 7, 1)
                )),
                ErrorCode.INVALID_REVIEW_CYCLE_PERIOD
        );

        ReviewCycleCreateRequest request = createRequest();
        when(reviewCycleRepository.existsByNameIgnoreCase(request.name())).thenReturn(true);
        assertReviewCycleError(
                () -> reviewCycleService.create(request),
                ErrorCode.DUPLICATE_REVIEW_CYCLE_NAME
        );

        when(reviewCycleRepository.existsByNameIgnoreCase(request.name())).thenReturn(false);
        when(reviewCycleRepository.existsByStartDateLessThanEqualAndEndDateGreaterThanEqual(
                request.endDate(),
                request.startDate()
        )).thenReturn(true);
        assertReviewCycleError(
                () -> reviewCycleService.create(request),
                ErrorCode.REVIEW_CYCLE_PERIOD_OVERLAP
        );

        ReviewCycleCreateRequest invalidTypeRequest = new ReviewCycleCreateRequest(
                "2026년 4분기 정기 재평가",
                "INVALID",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 12, 31)
        );
        assertReviewCycleError(
                () -> reviewCycleService.create(invalidTypeRequest),
                ErrorCode.INVALID_REVIEW_CYCLE_TYPE
        );
    }

    @Test
    void getAndGetAllReturnReviewCycles() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(reviewCycleRepository.findById(1L)).thenReturn(Optional.of(reviewCycle));
        when(reviewCycleRepository.findAllByOrderByStartDateDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(reviewCycle), pageable, 1));

        assertThat(reviewCycleService.get(1L).name()).isEqualTo("2026년 2분기 정기 재평가");
        assertThat(reviewCycleService.getAll(pageable).getContent())
                .extracting(ReviewCycleResponse::id)
                .containsExactly(1L);
    }

    @Test
    void updateChangesReviewCycle() {
        ReviewCycleUpdateRequest request = new ReviewCycleUpdateRequest(
                "2026년 상반기 수시 재평가",
                "AD_HOC",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 30)
        );
        when(reviewCycleRepository.findById(1L)).thenReturn(Optional.of(reviewCycle));

        ReviewCycleResponse response = reviewCycleService.update(1L, request);

        assertThat(response.name()).isEqualTo("2026년 상반기 수시 재평가");
        assertThat(response.type()).isEqualTo("AD_HOC");
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
                "2026년 3분기 정기 재평가",
                "QUARTERLY",
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
