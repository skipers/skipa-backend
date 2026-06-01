package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.BusinessOpinion;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewCycleType;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import com.skipers.skipa.domain.review.dto.request.ReviewSubmitRequest;
import com.skipers.skipa.domain.review.exception.ReviewException;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.dto.response.AssignedPatentResponse;
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

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignedPatentServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PatentService patentService;

    @Mock
    private BusinessPatentAccessValidator businessPatentAccessValidator;

    @InjectMocks
    private AssignedPatentService assignedPatentService;

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
    void getAllUsesAuthenticatedUsersDepartmentAndDescendingIdSort() {
        PageRequest pageable = PageRequest.of(0, 20);
        PageRequest sortedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));
        when(reviewRepository.findLatestAssignedByDepartmentId(1L, sortedPageable))
                .thenReturn(new PageImpl<>(List.of(review), sortedPageable, 1));

        assertThat(assignedPatentService.getAll(businessUser, pageable).getContent())
                .extracting(AssignedPatentResponse::id)
                .containsExactly(10L);
        verify(reviewRepository).findLatestAssignedByDepartmentId(1L, sortedPageable);
    }

    @Test
    void getRejectsSubmissionAssignedToAnotherDepartment() {
        doThrow(new PatentException(ErrorCode.FORBIDDEN))
                .when(businessPatentAccessValidator).validate(businessUser, 10L);

        assertPatentError(() -> assignedPatentService.get(businessUser, 10L), ErrorCode.FORBIDDEN);
    }

    @Test
    void getRejectsMissingSubmission() {
        when(reviewRepository.findFirstByPatentIdAndDepartmentIdOrderByIdDesc(10L, 1L)).thenReturn(Optional.empty());

        assertReviewError(() -> assignedPatentService.get(businessUser, 10L), ErrorCode.REVIEW_NOT_FOUND);
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
                () -> assignedPatentService.getAll(legalUser, PageRequest.of(0, 20)),
                ErrorCode.FORBIDDEN
        );
        verify(reviewRepository, never()).findLatestAssignedByDepartmentId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void submitUpdatesOpinionCommentStatusAndSubmittedAt() {
        when(reviewRepository.findFirstByPatentIdAndDepartmentIdAndStatusOrderByIdDesc(10L, 1L, ReviewStatus.미제출))
                .thenReturn(Optional.of(review));

        AssignedPatentResponse response = assignedPatentService.submit(
                businessUser,
                10L,
                new ReviewSubmitRequest("유지", "유지 요청")
        );

        assertThat(response.opinion()).isEqualTo("유지");
        assertThat(response.comment()).isEqualTo("유지 요청");
        assertThat(response.status()).isEqualTo("제출완료");
        assertThat(response.submittedAt()).isNotNull();
        assertThat(review.getOpinion()).isEqualTo(BusinessOpinion.유지);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.제출완료);
    }

    @Test
    void submitRejectsAlreadySubmittedRequest() {
        review.submit(BusinessOpinion.유지, "기존 의견", java.time.Instant.now());
        when(reviewRepository.findFirstByPatentIdAndDepartmentIdAndStatusOrderByIdDesc(10L, 1L, ReviewStatus.미제출))
                .thenReturn(Optional.empty());
        when(reviewRepository.findFirstByPatentIdAndDepartmentIdOrderByIdDesc(10L, 1L))
                .thenReturn(Optional.of(review));

        assertReviewError(
                () -> assignedPatentService.submit(
                        businessUser,
                        10L,
                        new ReviewSubmitRequest("포기", "변경 의견")
                ),
                ErrorCode.OPINION_ALREADY_SUBMITTED
        );
    }

    @Test
    void submitRejectsInvalidOpinion() {
        when(reviewRepository.findFirstByPatentIdAndDepartmentIdAndStatusOrderByIdDesc(10L, 1L, ReviewStatus.미제출))
                .thenReturn(Optional.of(review));

        assertReviewError(
                () -> assignedPatentService.submit(
                        businessUser,
                        10L,
                        new ReviewSubmitRequest("보류", null)
                ),
                ErrorCode.INVALID_REQUEST
        );
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
                .name("2026년 2분기 정기 재평가")
                .type(ReviewCycleType.QUARTERLY)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(reviewCycle, "id", 1L);
        return reviewCycle;
    }
}
