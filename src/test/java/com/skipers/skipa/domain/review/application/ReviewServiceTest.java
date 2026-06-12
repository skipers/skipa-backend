package com.skipers.skipa.domain.review.application;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.application.ApprovedPatentValidator;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.report.dao.ReportRepository;
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
import java.time.Instant;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewCycleRepository reviewCycleRepository;

    @Mock
    private PatentRepository patentRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ApprovedPatentValidator approvedPatentValidator;

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
                .year(2026)
                .quarter(2)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .deadline(LocalDate.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(reviewCycle, "id", 1L);

        patent = Patent.builder()
                .title("Patent")
                .applicationNumber("APP-1")
                .currentDepartment(department)
                .build();
        ReflectionTestUtils.setField(patent, "id", 10L);
        lenient().when(reportRepository.findFirstByPatentIdOrderByIdDesc(any())).thenReturn(Optional.empty());
        lenient().doNothing().when(approvedPatentValidator).validateApproved(any(Patent.class));
    }

    @Test
    void createSavesReviewWithPendingSubmissionStatus() {
        when(patentRepository.findById(10L)).thenReturn(Optional.of(patent));
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(any(), any()))
                .thenReturn(Optional.of(reviewCycle));
        when(reviewRepository.findByReviewCycleIdAndPatentIdAndDepartmentId(1L, 10L, 1L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", 100L);
            return review;
        });

        ReviewResponse response = reviewService.create(10L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.patentId()).isEqualTo(10L);
        assertThat(response.departmentId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.reviewCycleId()).isEqualTo(1L);
        assertThat(response.dueDate()).isEqualTo(reviewCycle.getDeadline());
        assertThat(response.opinion()).isNull();
        assertThat(response.submittedAt()).isNull();
        assertThat(response.checked()).isFalse();
    }

    @Test
    void createUsesReviewCycleDeadline() {
        when(patentRepository.findById(10L)).thenReturn(Optional.of(patent));
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(any(), any()))
                .thenReturn(Optional.of(reviewCycle));
        when(reviewRepository.findByReviewCycleIdAndPatentIdAndDepartmentId(1L, 10L, 1L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", 100L);
            return review;
        });

        ReviewResponse response = reviewService.create(10L);

        assertThat(response.dueDate()).isEqualTo(reviewCycle.getDeadline());
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
    void createRejectsInactiveDepartment() {
        department.deactivate();
        when(patentRepository.findById(10L)).thenReturn(Optional.of(patent));

        assertError(
                () -> reviewService.create(10L),
                ReviewException.class,
                ErrorCode.DEPARTMENT_INACTIVE
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
        when(reviewRepository.findByReviewCycleIdAndPatentIdAndDepartmentId(1L, 10L, 1L)).thenReturn(Optional.of(review()));

        assertError(
                () -> reviewService.create(10L),
                ReviewException.class,
                ErrorCode.DUPLICATE_REVIEW_REQUEST
        );
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createRequestsScheduledReviewTargetInsteadOfCreatingDuplicate() {
        Review scheduledReview = Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .status(ReviewStatus.SCHEDULED)
                .build();
        ReflectionTestUtils.setField(scheduledReview, "id", 100L);
        when(patentRepository.findById(10L)).thenReturn(Optional.of(patent));
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(any(), any()))
                .thenReturn(Optional.of(reviewCycle));
        when(reviewRepository.findByReviewCycleIdAndPatentIdAndDepartmentId(1L, 10L, 1L))
                .thenReturn(Optional.of(scheduledReview));

        ReviewResponse response = reviewService.create(10L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.dueDate()).isEqualTo(reviewCycle.getDeadline());
        assertThat(scheduledReview.getStatus()).isEqualTo(ReviewStatus.PENDING);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createBulkCreatesEligibleReviewsAndReturnsSkippedReasons() {
        Patent unassignedPatent = Patent.builder()
                .title("Unassigned Patent")
                .applicationNumber("APP-2")
                .build();
        ReflectionTestUtils.setField(unassignedPatent, "id", 20L);
        Department inactiveDepartment = Department.builder().name("비활성 부서").build();
        ReflectionTestUtils.setField(inactiveDepartment, "id", 2L);
        inactiveDepartment.deactivate();
        Patent inactiveDepartmentPatent = Patent.builder()
                .title("Inactive Department Patent")
                .applicationNumber("APP-3")
                .currentDepartment(inactiveDepartment)
                .build();
        ReflectionTestUtils.setField(inactiveDepartmentPatent, "id", 30L);
        Patent duplicatePatent = Patent.builder()
                .title("Duplicate Patent")
                .applicationNumber("APP-4")
                .currentDepartment(department)
                .build();
        ReflectionTestUtils.setField(duplicatePatent, "id", 40L);
        Review duplicateReview = Review.builder()
                .patent(duplicatePatent)
                .department(department)
                .reviewCycle(reviewCycle)
                .build();
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(any(), any()))
                .thenReturn(Optional.of(reviewCycle));
        when(patentRepository.findAllById(List.of(10L, 20L, 30L, 40L, 99L)))
                .thenReturn(List.of(patent, unassignedPatent, inactiveDepartmentPatent, duplicatePatent));
        when(reviewRepository.findAllByReviewCycleIdAndPatentIdIn(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.anyCollection()
        )).thenReturn(List.of(duplicateReview));

        BulkReviewCreateResponse response = reviewService.createBulk(
                new BulkReviewCreateRequest(List.of(10L, 20L, 30L, 40L, 99L, 10L))
        );

        assertThat(response.reviewCycleId()).isEqualTo(1L);
        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(4);
        assertThat(response.items())
                .extracting(BulkReviewCreateResponse.Item::patentId)
                .containsExactly(10L, 20L, 30L, 40L, 99L);
        assertThat(response.items())
                .extracting(BulkReviewCreateResponse.Item::status)
                .containsExactly("CREATED", "SKIPPED", "SKIPPED", "SKIPPED", "SKIPPED");
        assertThat(response.items())
                .extracting(BulkReviewCreateResponse.Item::reason)
                .containsExactly(
                        null,
                        "PATENT_DEPARTMENT_NOT_ASSIGNED",
                        "DEPARTMENT_INACTIVE",
                        "DUPLICATE_REVIEW_REQUEST",
                        "PATENT_NOT_FOUND"
                );
        verify(reviewRepository).saveAll(org.mockito.ArgumentMatchers.argThat(reviews -> {
            java.util.Iterator<Review> iterator = reviews.iterator();
            if (!iterator.hasNext()) {
                return false;
            }
            Review review = iterator.next();
            return review.getPatent().getId().equals(10L)
                    && reviewCycle.getDeadline().equals(review.getDueDate())
                    && !iterator.hasNext();
        }));
    }

    @Test
    void createBulkRejectsMissingActiveReviewCycle() {
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(any(), any()))
                .thenReturn(Optional.empty());

        assertError(
                () -> reviewService.createBulk(new BulkReviewCreateRequest(List.of(10L))),
                ReviewException.class,
                ErrorCode.ACTIVE_REVIEW_CYCLE_NOT_FOUND
        );

        verify(patentRepository, never()).findAllById(any());
        verify(reviewRepository, never()).saveAll(any());
    }

    @Test
    void createBulkSkipsMissingPatentsWithoutQueryingExistingReviews() {
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(any(), any()))
                .thenReturn(Optional.of(reviewCycle));
        when(patentRepository.findAllById(List.of(99L))).thenReturn(List.of());

        BulkReviewCreateResponse response = reviewService.createBulk(new BulkReviewCreateRequest(List.of(99L)));

        assertThat(response.createdCount()).isZero();
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.items().get(0).reason()).isEqualTo("PATENT_NOT_FOUND");
        verify(reviewRepository, never()).findAllByReviewCycleIdAndPatentIdIn(any(), any());
        verify(reviewRepository, never()).saveAll(any());
    }

    @Test
    void getAllUsesFiltersAndDefaultApplicationNumberSort() {
        PageRequest pageable = PageRequest.of(0, 20);
        PageRequest sortedPageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "patent.applicationNumber")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Review review = review();
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(any(), any()))
                .thenReturn(Optional.of(reviewCycle));
        when(reviewRepository.findAllByFilters(1L, ReviewStatus.PENDING, 1L, 10L, false, sortedPageable))
                .thenReturn(new PageImpl<>(List.of(review), sortedPageable, 1));

        assertThat(reviewService.getAll("PENDING", 1L, 10L, false, null, pageable).getContent())
                .extracting(ReviewResponse::id)
                .containsExactly(100L);
        verify(reviewRepository).findAllByFilters(1L, ReviewStatus.PENDING, 1L, 10L, false, sortedPageable);
    }

    @Test
    void getAllSortsByPatentFieldsWithDirection() {
        PageRequest pageable = PageRequest.of(0, 20);
        PageRequest sortedPageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "patent.applicationDate")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Review review = review();
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(any(), any()))
                .thenReturn(Optional.of(reviewCycle));
        when(reviewRepository.findAllByFilters(1L, null, null, null, null, sortedPageable))
                .thenReturn(new PageImpl<>(List.of(review), sortedPageable, 1));

        assertThat(reviewService.getAll(null, null, null, null, "applicationDate,asc", pageable).getContent())
                .extracting(ReviewResponse::id)
                .containsExactly(100L);
        verify(reviewRepository).findAllByFilters(1L, null, null, null, null, sortedPageable);
    }

    @Test
    void getAllRejectsInvalidStatus() {
        assertError(
                () -> reviewService.getAll("대기", null, null, null, null, PageRequest.of(0, 20)),
                ReviewException.class,
                ErrorCode.INVALID_REQUEST
        );
        verify(reviewRepository, never()).findAllByFilters(any(), any(), any(), any(), any(), any());
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

    @Test
    void confirmMarksSubmittedReviewAsChecked() {
        Review review = review();
        review.submit(BusinessOpinion.MAINTAIN, "유지", Instant.now());
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        ReviewConfirmResponse response = reviewService.confirm(100L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.checked()).isTrue();
        assertThat(review.isChecked()).isTrue();
    }

    @Test
    void confirmRejectsPendingReview() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review()));

        assertError(
                () -> reviewService.confirm(100L),
                ReviewException.class,
                ErrorCode.INVALID_REVIEW_STATUS
        );
    }

    @Test
    void confirmRejectsMissingReview() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.empty());

        assertError(
                () -> reviewService.confirm(100L),
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
