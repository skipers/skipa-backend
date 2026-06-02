package com.skipers.skipa.domain.review.application;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewCycleType;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import com.skipers.skipa.domain.review.dto.response.ReviewResponse;
import com.skipers.skipa.domain.review.exception.ReviewException;
import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewCycleRepository reviewCycleRepository;

    @Mock
    private PatentRepository patentRepository;

    @InjectMocks
    private ReviewService reviewService;

    private Patent patent;
    private Department department;
    private ReviewCycle reviewCycle;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .name("통신")
                .build();
        ReflectionTestUtils.setField(department, "id", 1L);
        reviewCycle = ReviewCycle.builder()
                .name("2026년 2분기 정기 재평가")
                .type(ReviewCycleType.QUARTERLY)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(reviewCycle, "id", 1L);

        patent = Patent.builder()
                .title("Patent")
                .applicationNumber("APP-1")
                .currentDepartment(department)
                .build();
        ReflectionTestUtils.setField(patent, "id", 10L);
    }

    @Test
    void createSavesReviewWithPendingSubmissionStatus() {
        when(patentRepository.findById(10L)).thenReturn(Optional.of(patent));
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(any(), any()))
                .thenReturn(Optional.of(reviewCycle));
        when(reviewRepository.existsByReviewCycleIdAndPatentIdAndDepartmentId(1L, 10L, 1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", 100L);
            return review;
        });

        ReviewResponse response = reviewService.create(10L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.patentId()).isEqualTo(10L);
        assertThat(response.departmentId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("미제출");
        assertThat(response.reviewCycleId()).isEqualTo(1L);
        assertThat(response.dueDate()).isEqualTo(reviewCycle.getEndDate());
        assertThat(response.opinion()).isNull();
        assertThat(response.submittedAt()).isNull();
    }

    @Test
    void createRejectsMissingPatent() {
        when(patentRepository.findById(10L)).thenReturn(Optional.empty());

        assertError(
                () -> reviewService.create(10L),
                PatentException.class,
                ErrorCode.PATENT_NOT_FOUND
        );
    }

    @Test
    void createRejectsPatentWithoutAssignedDepartment() {
        Patent unassignedPatent = Patent.builder()
                .title("Patent")
                .applicationNumber("APP-1")
                .build();
        when(patentRepository.findById(10L)).thenReturn(Optional.of(unassignedPatent));

        assertError(
                () -> reviewService.create(10L),
                ReviewException.class,
                ErrorCode.PATENT_DEPARTMENT_NOT_ASSIGNED
        );
        verify(reviewCycleRepository, never())
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(any(), any());
    }

    @Test
    void createRejectsMissingActiveReviewCycle() {
        when(patentRepository.findById(10L)).thenReturn(Optional.of(patent));
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(any(), any()))
                .thenReturn(Optional.empty());

        assertError(
                () -> reviewService.create(10L),
                ReviewException.class,
                ErrorCode.ACTIVE_REVIEW_CYCLE_NOT_FOUND
        );
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateReviewRequestInSameCycle() {
        when(patentRepository.findById(10L)).thenReturn(Optional.of(patent));
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(any(), any()))
                .thenReturn(Optional.of(reviewCycle));
        when(reviewRepository.existsByReviewCycleIdAndPatentIdAndDepartmentId(1L, 10L, 1L)).thenReturn(true);

        assertError(
                () -> reviewService.create(10L),
                ReviewException.class,
                ErrorCode.DUPLICATE_REVIEW_REQUEST
        );
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void getAllUsesFiltersAndDescendingIdSort() {
        PageRequest pageable = PageRequest.of(0, 20);
        PageRequest sortedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));
        Review review = review();
        when(reviewRepository.findAllByFilters(ReviewStatus.미제출, 1L, 10L, sortedPageable))
                .thenReturn(new PageImpl<>(List.of(review), sortedPageable, 1));

        assertThat(reviewService.getAll("미제출", 1L, 10L, pageable).getContent())
                .extracting(ReviewResponse::id)
                .containsExactly(100L);
        verify(reviewRepository).findAllByFilters(ReviewStatus.미제출, 1L, 10L, sortedPageable);
    }

    @Test
    void getAllRejectsInvalidStatus() {
        assertError(
                () -> reviewService.getAll("대기", null, null, PageRequest.of(0, 20)),
                ReviewException.class,
                ErrorCode.INVALID_REQUEST
        );
        verify(reviewRepository, never()).findAllByFilters(any(), any(), any(), any());
    }

    @Test
    void getReturnsReview() {
        Review review = review();
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        assertThat(reviewService.get(100L).id()).isEqualTo(100L);
    }

    @Test
    void getRejectsMissingReview() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.empty());

        assertError(
                () -> reviewService.get(100L),
                ReviewException.class,
                ErrorCode.REVIEW_NOT_FOUND
        );
    }

    private Review review() {
        Review review = Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .build();
        ReflectionTestUtils.setField(review, "id", 100L);
        return review;
    }

    private <T extends BusinessException> void assertError(
            Runnable invocation,
            Class<T> exceptionType,
            ErrorCode errorCode
    ) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(exceptionType,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
