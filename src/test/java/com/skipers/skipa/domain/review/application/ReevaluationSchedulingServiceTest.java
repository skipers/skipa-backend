package com.skipers.skipa.domain.review.application;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentAnnuity;
import com.skipers.skipa.domain.patent.domain.PatentAnnuityStatus;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ReevaluationSchedulingServiceTest {

    @Mock
    private PatentAnnuityRepository patentAnnuityRepository;

    @Mock
    private ReviewCycleRepository reviewCycleRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReevaluationSchedulingService reevaluationSchedulingService;

    @Test
    void scheduleNextQuarterReevaluationsCreatesScheduledReviewsForUnpaidAnnuities() {
        Department department = Department.builder().name("통신").build();
        ReflectionTestUtils.setField(department, "id", 1L);
        Patent patent = Patent.builder()
                .title("Patent")
                .applicationNumber("APP-1")
                .currentDepartment(department)
                .build();
        ReflectionTestUtils.setField(patent, "id", 10L);
        ReviewCycle reviewCycle = ReviewCycle.builder()
                .year(LocalDate.now().getYear())
                .quarter(1)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(reviewCycle, "id", 100L);
        PatentAnnuity annuity = PatentAnnuity.builder()
                .patent(patent)
                .startYear(3)
                .dueDate(nextQuarterStart())
                .status(PatentAnnuityStatus.UNPAID)
                .build();
        ReflectionTestUtils.setField(annuity, "id", 1000L);

        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                any(),
                any()
        )).thenReturn(Optional.of(reviewCycle));
        when(patentAnnuityRepository.findByStatusAndDueDateBetween(
                any(),
                any(),
                any()
        )).thenReturn(List.of(annuity));

        int created = reevaluationSchedulingService.scheduleNextQuarterReevaluations();

        assertThat(created).isEqualTo(1);
        verify(reviewRepository).save(argThat(review ->
                review.getPatent() == patent
                        && review.getDepartment() == department
                        && review.getReviewCycle() == reviewCycle
                        && review.getPatentAnnuity() == annuity
                        && review.getStatus() == ReviewStatus.SCHEDULED
        ));
    }

    @Test
    void scheduleNextQuarterReevaluationsSkipsWhenActiveCycleIsMissing() {
        when(reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                any(),
                any()
        )).thenReturn(Optional.empty());

        int created = reevaluationSchedulingService.scheduleNextQuarterReevaluations();

        assertThat(created).isZero();
        verify(patentAnnuityRepository, never()).findByStatusAndDueDateBetween(any(), any(), any());
        verify(reviewRepository, never()).save(any());
    }

    private LocalDate nextQuarterStart() {
        YearMonth currentMonth = YearMonth.from(LocalDate.now());
        int currentQuarter = ((currentMonth.getMonthValue() - 1) / 3) + 1;
        int nextQuarter = currentQuarter == 4 ? 1 : currentQuarter + 1;
        int year = currentQuarter == 4 ? currentMonth.getYear() + 1 : currentMonth.getYear();
        return LocalDate.of(year, (nextQuarter - 1) * 3 + 1, 1);
    }
}
