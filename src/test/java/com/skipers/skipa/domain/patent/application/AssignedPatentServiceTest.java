package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.opinion.dao.OpinionSubmissionRepository;
import com.skipers.skipa.domain.opinion.domain.BusinessOpinion;
import com.skipers.skipa.domain.opinion.domain.OpinionSubmission;
import com.skipers.skipa.domain.opinion.domain.OpinionSubmissionStatus;
import com.skipers.skipa.domain.opinion.dto.request.OpinionSubmissionSubmitRequest;
import com.skipers.skipa.domain.opinion.exception.OpinionSubmissionException;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.dto.response.AssignedPatentResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignedPatentServiceTest {

    @Mock
    private OpinionSubmissionRepository opinionSubmissionRepository;

    @Mock
    private PatentService patentService;

    @InjectMocks
    private AssignedPatentService assignedPatentService;

    private User businessUser;
    private OpinionSubmission opinionSubmission;

    @BeforeEach
    void setUp() {
        Department department = department(1L, "통신");
        Patent patent = Patent.builder()
                .title("Patent")
                .applicationNumber("APP-1")
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
        opinionSubmission = OpinionSubmission.builder()
                .patent(patent)
                .department(department)
                .build();
        ReflectionTestUtils.setField(opinionSubmission, "id", 100L);
    }

    @Test
    void getAllUsesAuthenticatedUsersDepartmentAndDescendingIdSort() {
        PageRequest pageable = PageRequest.of(0, 20);
        PageRequest sortedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));
        when(opinionSubmissionRepository.findByDepartmentId(1L, sortedPageable))
                .thenReturn(new PageImpl<>(List.of(opinionSubmission), sortedPageable, 1));

        assertThat(assignedPatentService.getAll(businessUser, pageable).getContent())
                .extracting(AssignedPatentResponse::id)
                .containsExactly(10L);
        verify(opinionSubmissionRepository).findByDepartmentId(1L, sortedPageable);
    }

    @Test
    void getRejectsSubmissionAssignedToAnotherDepartment() {
        when(opinionSubmissionRepository.findByPatentIdAndDepartmentId(10L, 1L)).thenReturn(Optional.empty());
        when(opinionSubmissionRepository.existsByPatentId(10L)).thenReturn(true);

        assertOpinionError(() -> assignedPatentService.get(businessUser, 10L), ErrorCode.FORBIDDEN);
    }

    @Test
    void getRejectsMissingSubmission() {
        when(opinionSubmissionRepository.findByPatentIdAndDepartmentId(10L, 1L)).thenReturn(Optional.empty());
        when(opinionSubmissionRepository.existsByPatentId(10L)).thenReturn(false);

        assertOpinionError(() -> assignedPatentService.get(businessUser, 10L), ErrorCode.DECISION_NOT_FOUND);
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

        assertOpinionError(
                () -> assignedPatentService.getAll(legalUser, PageRequest.of(0, 20)),
                ErrorCode.FORBIDDEN
        );
        verify(opinionSubmissionRepository, never()).findByDepartmentId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void submitUpdatesOpinionCommentStatusAndSubmittedAt() {
        when(opinionSubmissionRepository.findByPatentIdAndDepartmentId(10L, 1L))
                .thenReturn(Optional.of(opinionSubmission));

        AssignedPatentResponse response = assignedPatentService.submit(
                businessUser,
                10L,
                new OpinionSubmissionSubmitRequest("유지", "유지 요청")
        );

        assertThat(response.opinion()).isEqualTo("유지");
        assertThat(response.comment()).isEqualTo("유지 요청");
        assertThat(response.status()).isEqualTo("제출완료");
        assertThat(response.submittedAt()).isNotNull();
        assertThat(opinionSubmission.getOpinion()).isEqualTo(BusinessOpinion.유지);
        assertThat(opinionSubmission.getStatus()).isEqualTo(OpinionSubmissionStatus.제출완료);
    }

    @Test
    void submitRejectsAlreadySubmittedRequest() {
        opinionSubmission.submit(BusinessOpinion.유지, "기존 의견", java.time.Instant.now());
        when(opinionSubmissionRepository.findByPatentIdAndDepartmentId(10L, 1L))
                .thenReturn(Optional.of(opinionSubmission));

        assertOpinionError(
                () -> assignedPatentService.submit(
                        businessUser,
                        10L,
                        new OpinionSubmissionSubmitRequest("포기", "변경 의견")
                ),
                ErrorCode.DECISION_ALREADY_SUBMITTED
        );
    }

    @Test
    void submitRejectsInvalidOpinion() {
        when(opinionSubmissionRepository.findByPatentIdAndDepartmentId(10L, 1L))
                .thenReturn(Optional.of(opinionSubmission));

        assertOpinionError(
                () -> assignedPatentService.submit(
                        businessUser,
                        10L,
                        new OpinionSubmissionSubmitRequest("보류", null)
                ),
                ErrorCode.INVALID_REQUEST
        );
    }

    private void assertOpinionError(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(OpinionSubmissionException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }

    private Department department(Long id, String name) {
        Department department = Department.builder().name(name).build();
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }
}
