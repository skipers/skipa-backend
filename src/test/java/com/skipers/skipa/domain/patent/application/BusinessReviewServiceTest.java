package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.domain.ReportStatus;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.BusinessOpinion;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import com.skipers.skipa.domain.review.dto.request.ReviewSubmitRequest;
import com.skipers.skipa.domain.review.exception.ReviewException;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.dto.response.BusinessReviewHistoryResponse;
import com.skipers.skipa.domain.patent.dto.response.BusinessReviewResponse;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewCycleRepository reviewCycleRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PatentService patentService;

    @Mock
    private BusinessPatentAccessValidator businessPatentAccessValidator;

    @InjectMocks
    private BusinessReviewService businessReviewService;

    private User businessUser;
    private Review review;

    @BeforeEach
    void setUp() {
        Department department = department(1L, "통신");
        Patent patent = Patent.builder()
                .title("Patent")
                .applicationNumber("APP-1")
                .currentDepartment(department)
                .build();
        ReflectionTestUtils.setField(patent, "id", 10L);

        businessUser = User.createActive(
                "business",
                "Business",
                "business@example.com",
                "password",
                UserRole.BUSINESS,
                department
        );
        review = Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle())
                .build();
        ReflectionTestUtils.setField(review, "id", 100L);
    }

    @Test
    void getAllUsesAuthenticatedUsersDepartmentAndDefaultApplicationNumberSort() {
        PageRequest pageable = PageRequest.of(0, 20);
        PageRequest sortedPageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "patent.applicationNumber")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Report report = Report.builder()
                .patent(review.getPatent())
                .totalScore(new BigDecimal("92.50"))
                .valueGrade("S")
                .status(ReportStatus.COMPLETED)
                .build();
        ReflectionTestUtils.setField(report, "id", 1000L);
        stubActiveReviewCycle();
        when(reviewRepository.findLatestBusinessReviewsByReviewCycleIdAndDepartmentId(1L, 1L, null, null, null, null, sortedPageable))
                .thenReturn(new PageImpl<>(List.of(review), sortedPageable, 1));
        when(reportRepository.findAllByStatus(ReportStatus.COMPLETED)).thenReturn(List.of(report));

        List<BusinessReviewResponse> responses = businessReviewService.getAll(
                businessUser,
                null,
                null,
                null,
                null,
                null,
                pageable
        ).getContent();

        assertThat(responses)
                .extracting(BusinessReviewResponse::id)
                .containsExactly(100L);
        assertThat(responses)
                .extracting(BusinessReviewResponse::patentId)
                .containsExactly(10L);
        assertThat(responses.get(0))
                .extracting(BusinessReviewResponse::totalScore, BusinessReviewResponse::valueGrade)
                .containsExactly(new BigDecimal("92.50"), "S");
        verify(reviewRepository).findLatestBusinessReviewsByReviewCycleIdAndDepartmentId(1L, 1L, null, null, null, null, sortedPageable);
    }

    @Test
    void getSummaryReturnsActiveReviewCycleAndSubmissionKpi() {
        ReviewCycle reviewCycle = reviewCycle();
        ReflectionTestUtils.setField(reviewCycle, "id", 1L);
        Review submittedReview = Review.builder()
                .patent(review.getPatent())
                .department(review.getDepartment())
                .reviewCycle(reviewCycle)
                .status(ReviewStatus.SUBMITTED)
                .build();
        Review scheduledReview = Review.builder()
                .patent(review.getPatent())
                .department(review.getDepartment())
                .reviewCycle(reviewCycle)
                .status(ReviewStatus.SCHEDULED)
                .build();
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(Optional.of(reviewCycle));
        when(reviewRepository.findAllByReviewCycleId(1L)).thenReturn(List.of(review, submittedReview, scheduledReview));

        var response = businessReviewService.getSummary(businessUser);

        assertThat(response.reviewCycle().id()).isEqualTo(1L);
        assertThat(response.kpi().submitted()).isEqualTo(1);
        assertThat(response.kpi().notSubmitted()).isEqualTo(1);
    }

    @Test
    void getAllAppliesStatusOpinionAndSubmittedDateFilters() {
        PageRequest pageable = PageRequest.of(0, 20);
        PageRequest sortedPageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "patent.applicationNumber")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        LocalDate submittedFrom = LocalDate.of(2026, 6, 1);
        LocalDate submittedTo = LocalDate.of(2026, 6, 30);
        stubActiveReviewCycle();
        when(reviewRepository.findLatestBusinessReviewsByReviewCycleIdAndDepartmentId(
                1L,
                1L,
                ReviewStatus.SUBMITTED,
                BusinessOpinion.MAINTAIN,
                submittedFrom.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                submittedTo.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                sortedPageable
        )).thenReturn(new PageImpl<>(List.of(review), sortedPageable, 1));

        assertThat(businessReviewService.getAll(
                businessUser,
                "SUBMITTED",
                "MAINTAIN",
                submittedFrom,
                submittedTo,
                null,
                pageable
        ).getContent()).hasSize(1);
    }

    @Test
    void getAllSortsByPatentFieldsWithDirection() {
        PageRequest pageable = PageRequest.of(0, 20);
        PageRequest sortedPageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "patent.expiryDate")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        stubActiveReviewCycle();
        when(reviewRepository.findLatestBusinessReviewsByReviewCycleIdAndDepartmentId(
                1L,
                1L,
                null,
                null,
                null,
                null,
                sortedPageable
        )).thenReturn(new PageImpl<>(List.of(review), sortedPageable, 1));

        assertThat(businessReviewService.getAll(
                businessUser,
                null,
                null,
                null,
                null,
                "expiryDate,desc",
                pageable
        ).getContent()).hasSize(1);
        verify(reviewRepository).findLatestBusinessReviewsByReviewCycleIdAndDepartmentId(
                1L,
                1L,
                null,
                null,
                null,
                null,
                sortedPageable
        );
    }

    @Test
    void getHistoryReturnsSubmittedPastReviewsWithYearQuarterFilters() {
        PageRequest pageable = PageRequest.of(0, 20);
        ReviewCycle pastCycle = ReviewCycle.builder()
                .year(2025)
                .quarter(4)
                .startDate(LocalDate.now().minusMonths(6))
                .endDate(LocalDate.now().minusMonths(3))
                .build();
        ReflectionTestUtils.setField(pastCycle, "id", 2L);
        Review pastReview = Review.builder()
                .patent(review.getPatent())
                .department(review.getDepartment())
                .reviewCycle(pastCycle)
                .status(ReviewStatus.SUBMITTED)
                .build();
        ReflectionTestUtils.setField(pastReview, "id", 200L);
        Report report = Report.builder()
                .patent(review.getPatent())
                .totalScore(new BigDecimal("88.00"))
                .valueGrade("A")
                .status(ReportStatus.COMPLETED)
                .build();
        ReflectionTestUtils.setField(report, "id", 1000L);
        when(reviewRepository.findSubmittedBusinessReviewHistory(
                1L,
                LocalDate.now(),
                2025,
                4,
                pageable
        )).thenReturn(new PageImpl<>(List.of(pastReview), pageable, 1));
        when(reportRepository.findAllByStatus(ReportStatus.COMPLETED)).thenReturn(List.of(report));

        List<BusinessReviewHistoryResponse> responses = businessReviewService.getHistory(
                businessUser,
                2025,
                4,
                pageable
        ).getContent();

        assertThat(responses)
                .extracting(BusinessReviewHistoryResponse::id)
                .containsExactly(200L);
        assertThat(responses.get(0).reviewCycle().year()).isEqualTo(2025);
        assertThat(responses.get(0).reviewCycle().quarter()).isEqualTo(4);
        assertThat(responses.get(0))
                .extracting(BusinessReviewHistoryResponse::totalScore, BusinessReviewHistoryResponse::valueGrade)
                .containsExactly(new BigDecimal("88.00"), "A");
    }

    @Test
    void getHistoryRejectsQuarterWithoutYear() {
        assertReviewError(
                () -> businessReviewService.getHistory(businessUser, null, 1, PageRequest.of(0, 20)),
                ErrorCode.INVALID_REQUEST
        );
    }

    @Test
    void getRejectsSubmissionAssignedToAnotherDepartment() {
        stubActiveReviewCycle();
        doThrow(new PatentException(ErrorCode.FORBIDDEN))
                .when(businessPatentAccessValidator).validate(businessUser, 10L);

        assertPatentError(() -> businessReviewService.get(businessUser, 10L), ErrorCode.FORBIDDEN);
    }

    @Test
    void getRejectsMissingSubmission() {
        stubActiveReviewCycle();
        when(reviewRepository.findFirstByReviewCycleIdAndPatentIdAndDepartmentIdAndStatusInOrderByIdDesc(1L, 10L, 1L, java.util.List.of(ReviewStatus.PENDING, ReviewStatus.OVERDUE, ReviewStatus.SUBMITTED))).thenReturn(Optional.empty());

        assertReviewError(() -> businessReviewService.get(businessUser, 10L), ErrorCode.REVIEW_NOT_FOUND);
    }

    @Test
    void getAllRejectsUserWithoutDepartment() {
        User legalUser = User.createActive(
                "legal",
                "Legal",
                "legal@example.com",
                "password",
                UserRole.LEGAL,
                null
        );

        assertReviewError(
                () -> businessReviewService.getAll(legalUser, null, null, null, null, null, PageRequest.of(0, 20)),
                ErrorCode.FORBIDDEN
        );
        verify(reviewRepository, never()).findLatestBusinessReviewsByReviewCycleIdAndDepartmentId(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void getAllRejectsInvalidStatusFilter() {
        assertReviewError(
                () -> businessReviewService.getAll(businessUser, "DONE", null, null, null, null, PageRequest.of(0, 20)),
                ErrorCode.INVALID_REQUEST
        );
    }

    @Test
    void getAllRejectsInvalidOpinionFilter() {
        assertReviewError(
                () -> businessReviewService.getAll(businessUser, null, "HOLD", null, null, null, PageRequest.of(0, 20)),
                ErrorCode.INVALID_REQUEST
        );
    }

    @Test
    void submitUpdatesOpinionCommentStatusAndSubmittedAt() {
        stubActiveReviewCycle();
        when(reviewRepository.findFirstByReviewCycleIdAndPatentIdAndDepartmentIdAndStatusInOrderByIdDesc(1L, 10L, 1L, java.util.List.of(ReviewStatus.PENDING, ReviewStatus.OVERDUE, ReviewStatus.SUBMITTED)))
                .thenReturn(Optional.of(review));

        BusinessReviewResponse response = businessReviewService.submit(
                businessUser,
                10L,
                new ReviewSubmitRequest("MAINTAIN", "유지 요청")
        );

        assertThat(response.opinion()).isEqualTo("MAINTAIN");
        assertThat(response.comment()).isEqualTo("유지 요청");
        assertThat(response.status()).isEqualTo("SUBMITTED");
        assertThat(response.submittedAt()).isNotNull();
        assertThat(review.getOpinion()).isEqualTo(BusinessOpinion.MAINTAIN);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.SUBMITTED);
    }

    @Test
    void submitRejectsAlreadySubmittedRequest() {
        review.submit(BusinessOpinion.MAINTAIN, "기존 의견", java.time.Instant.now());
        stubActiveReviewCycle();
        when(reviewRepository.findFirstByReviewCycleIdAndPatentIdAndDepartmentIdAndStatusInOrderByIdDesc(1L, 10L, 1L, java.util.List.of(ReviewStatus.PENDING, ReviewStatus.OVERDUE, ReviewStatus.SUBMITTED)))
                .thenReturn(Optional.of(review));

        assertReviewError(
                () -> businessReviewService.submit(
                        businessUser,
                        10L,
                        new ReviewSubmitRequest("ABANDON", "변경 의견")
                ),
                ErrorCode.OPINION_ALREADY_SUBMITTED
        );
    }

    @Test
    void submitRejectsInvalidOpinion() {
        stubActiveReviewCycle();
        when(reviewRepository.findFirstByReviewCycleIdAndPatentIdAndDepartmentIdAndStatusInOrderByIdDesc(1L, 10L, 1L, java.util.List.of(ReviewStatus.PENDING, ReviewStatus.OVERDUE, ReviewStatus.SUBMITTED)))
                .thenReturn(Optional.of(review));

        assertReviewError(
                () -> businessReviewService.submit(
                        businessUser,
                        10L,
                        new ReviewSubmitRequest("보류", null)
                ),
                ErrorCode.INVALID_REQUEST
        );
    }

    @Test
    void submitAllowsOverdueRequestAfterDueDate() {
        Review overdueReview = Review.builder()
                .patent(review.getPatent())
                .department(review.getDepartment())
                .reviewCycle(reviewCycle())
                .status(ReviewStatus.OVERDUE)
                .dueDate(LocalDate.now().minusDays(1))
                .build();
        stubActiveReviewCycle();
        when(reviewRepository.findFirstByReviewCycleIdAndPatentIdAndDepartmentIdAndStatusInOrderByIdDesc(1L, 10L, 1L, java.util.List.of(ReviewStatus.PENDING, ReviewStatus.OVERDUE, ReviewStatus.SUBMITTED)))
                .thenReturn(Optional.of(overdueReview));

        BusinessReviewResponse response = businessReviewService.submit(
                businessUser,
                10L,
                new ReviewSubmitRequest("MAINTAIN", null)
        );

        assertThat(response.status()).isEqualTo("SUBMITTED");
        assertThat(response.opinion()).isEqualTo("MAINTAIN");
    }

    private void assertReviewError(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(ReviewException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }

    private void assertPatentError(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(PatentException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }

    private Department department(Long id, String name) {
        Department department = Department.builder().name(name).build();
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }

    private ReviewCycle reviewCycle() {
        ReviewCycle reviewCycle = ReviewCycle.builder()
                .year(2026)
                .quarter(2)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(reviewCycle, "id", 1L);
        return reviewCycle;
    }

    private void stubActiveReviewCycle() {
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(Optional.of(reviewCycle()));
    }
}
