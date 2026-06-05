package com.skipers.skipa.domain.dashboard.application;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewCycleType;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PatentRepository patentRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewCycleRepository reviewCycleRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getSummaryReturnsZerosWhenActiveReviewCycleDoesNotExist() {
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(LocalDate.now(), LocalDate.now()))
                .thenReturn(Optional.empty());

        var response = dashboardService.getSummary();

        assertThat(response.progressRate()).isZero();
        assertThat(response.delayed()).isZero();
        assertThat(response.kpi().requested()).isZero();
        assertThat(response.kpi().reviewing()).isZero();
        assertThat(response.kpi().decided()).isZero();
    }

    @Test
    void getSummaryAggregatesActiveReviewCycleCounts() {
        ReviewCycle reviewCycle = ReviewCycle.builder()
                .name("2026 2Q")
                .type(ReviewCycleType.QUARTERLY)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(reviewCycle, "id", 1L);
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(LocalDate.now(), LocalDate.now()))
                .thenReturn(Optional.of(reviewCycle));
        when(reviewRepository.countByReviewCycleId(1L)).thenReturn(10L);
        when(reviewRepository.countByReviewCycleIdAndStatus(1L, ReviewStatus.PENDING)).thenReturn(4L);
        when(reviewRepository.countByReviewCycleIdAndStatus(1L, ReviewStatus.SUBMITTED)).thenReturn(6L);
        when(reviewRepository.countByReviewCycleIdAndStatusAndDueDateBefore(1L, ReviewStatus.PENDING, LocalDate.now())).thenReturn(2L);

        var response = dashboardService.getSummary();

        assertThat(response.progressRate()).isEqualTo(60);
        assertThat(response.delayed()).isEqualTo(2);
        assertThat(response.kpi().requested()).isEqualTo(10);
        assertThat(response.kpi().reviewing()).isEqualTo(4);
        assertThat(response.kpi().decided()).isEqualTo(6);
    }

    @Test
    void getAssignmentCountsPatentsAndCompletedReviews() {
        ReviewCycle reviewCycle = ReviewCycle.builder()
                .name("2026 2Q")
                .type(ReviewCycleType.QUARTERLY)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(reviewCycle, "id", 1L);
        when(patentRepository.countByCurrentDepartmentIsNull()).thenReturn(3L);
        when(patentRepository.countByCurrentDepartmentIsNotNull()).thenReturn(7L);
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(LocalDate.now(), LocalDate.now()))
                .thenReturn(Optional.of(reviewCycle));
        when(reviewRepository.countByReviewCycleIdAndStatus(1L, ReviewStatus.SUBMITTED)).thenReturn(5L);

        var response = dashboardService.getAssignment();

        assertThat(response.unassigned()).isEqualTo(3);
        assertThat(response.assigned()).isEqualTo(7);
        assertThat(response.completed()).isEqualTo(5);
    }

    @Test
    void getDistributionAggregatesTechFieldAndExpiryQuarter() {
        Patent patentA = Patent.builder()
                .title("A")
                .applicationNumber("APP-A")
                .techField("반도체")
                .expiryDate(LocalDate.of(2026, 2, 10))
                .build();
        Patent patentB = Patent.builder()
                .title("B")
                .applicationNumber("APP-B")
                .techField("반도체")
                .expiryDate(LocalDate.of(2026, 5, 20))
                .build();
        Patent patentC = Patent.builder()
                .title("C")
                .applicationNumber("APP-C")
                .techField("배터리")
                .expiryDate(LocalDate.of(2026, 5, 21))
                .build();
        when(patentRepository.findByTechFieldIsNotNull()).thenReturn(List.of(patentA, patentB, patentC));
        when(patentRepository.findByExpiryDateIsNotNullAndExpiryDateGreaterThanEqual(LocalDate.now()))
                .thenReturn(List.of(patentA, patentB, patentC));

        var response = dashboardService.getDistribution();

        assertThat(response.byTechField()).hasSize(2);
        assertThat(response.byTechField().get(0).name()).isEqualTo("반도체");
        assertThat(response.byTechField().get(0).count()).isEqualTo(2);
        assertThat(response.byTechField().get(1).name()).isEqualTo("배터리");
        assertThat(response.byTechField().get(1).count()).isEqualTo(1);
        assertThat(response.byExpiryQuarter()).hasSize(2);
        assertThat(response.byExpiryQuarter().get(0).quarter()).isEqualTo("2026Q1");
        assertThat(response.byExpiryQuarter().get(0).count()).isEqualTo(1);
        assertThat(response.byExpiryQuarter().get(1).quarter()).isEqualTo("2026Q2");
        assertThat(response.byExpiryQuarter().get(1).count()).isEqualTo(2);
    }

    @Test
    void getDepartmentsAggregatesDepartmentProgress() {
        ReviewCycle reviewCycle = ReviewCycle.builder()
                .name("2026 2Q")
                .type(ReviewCycleType.QUARTERLY)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(reviewCycle, "id", 1L);
        Department department = Department.builder().name("반도체 사업부").build();
        ReflectionTestUtils.setField(department, "id", 2L);
        Patent patentA = Patent.builder().title("A").applicationNumber("APP-A").currentDepartment(department).build();
        Patent patentB = Patent.builder().title("B").applicationNumber("APP-B").currentDepartment(department).build();
        Review reviewA = Review.builder().patent(patentA).department(department).reviewCycle(reviewCycle).status(ReviewStatus.PENDING).build();
        Review reviewB = Review.builder().patent(patentB).department(department).reviewCycle(reviewCycle).status(ReviewStatus.SUBMITTED).build();

        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(LocalDate.now(), LocalDate.now()))
                .thenReturn(Optional.of(reviewCycle));
        when(reviewRepository.findAllByReviewCycleId(1L)).thenReturn(List.of(reviewA, reviewB));

        var response = dashboardService.getDepartments();

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).departmentId()).isEqualTo(2L);
        assertThat(response.items().get(0).departmentName()).isEqualTo("반도체 사업부");
        assertThat(response.items().get(0).assigned()).isEqualTo(2);
        assertThat(response.items().get(0).reviewing()).isEqualTo(1);
        assertThat(response.items().get(0).decided()).isEqualTo(1);
        assertThat(response.items().get(0).progressRate()).isEqualTo(50);
    }
}
