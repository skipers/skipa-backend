package com.skipers.skipa.domain.review.application;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.application.ApprovedPatentValidator;
import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentAnnuity;
import com.skipers.skipa.domain.patent.domain.PatentAnnuityStatus;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewTargetSchedulingService {

    private final PatentAnnuityRepository patentAnnuityRepository;
    private final ReviewCycleRepository reviewCycleRepository;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;
    private final ApprovedPatentValidator approvedPatentValidator;

    @Transactional
    public int scheduleNextQuarterReviewTargets() {
        QuarterRange nextQuarter = nextQuarter(LocalDate.now());
        ReviewCycle reviewCycle = reviewCycleRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        LocalDate.now(),
                        LocalDate.now()
                )
                .orElse(null);
        if (reviewCycle == null) {
            return 0;
        }

        int created = 0;
        for (PatentAnnuity annuity : patentAnnuityRepository.findByStatusAndDueDateBetween(
                PatentAnnuityStatus.UNPAID,
                nextQuarter.startDate(),
                nextQuarter.endDate()
        )) {
            Patent patent = annuity.getPatent();
            if (!isApprovedPatent(patent)) {
                continue;
            }
            Department department = patent.getCurrentDepartment();
            if (department == null || department.isInactive()) {
                continue;
            }
            if (reviewRepository.existsByReviewCycleIdAndPatentIdAndDepartmentId(
                    reviewCycle.getId(),
                    patent.getId(),
                    department.getId()
            )) {
                continue;
            }

            reviewRepository.save(Review.builder()
                    .patent(patent)
                    .department(department)
                    .reviewCycle(reviewCycle)
                    .report(findLatestReport(patent.getId()))
                    .patentAnnuity(annuity)
                    .status(ReviewStatus.SCHEDULED)
                    .build());
            created++;
        }

        return created;
    }

    private Report findLatestReport(Long patentId) {
        return reportRepository.findFirstByPatentIdOrderByIdDesc(patentId).orElse(null);
    }

    private boolean isApprovedPatent(Patent patent) {
        try {
            approvedPatentValidator.validateApproved(patent);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private QuarterRange nextQuarter(LocalDate today) {
        YearMonth currentMonth = YearMonth.from(today);
        int currentQuarter = ((currentMonth.getMonthValue() - 1) / 3) + 1;
        int quarter = currentQuarter == 4 ? 1 : currentQuarter + 1;
        int year = currentQuarter == 4 ? currentMonth.getYear() + 1 : currentMonth.getYear();
        int startMonth = (quarter - 1) * 3 + 1;
        LocalDate startDate = LocalDate.of(year, startMonth, 1);
        LocalDate endDate = startDate.plusMonths(3).minusDays(1);
        return new QuarterRange(year, quarter, startDate, endDate);
    }

    private record QuarterRange(
            int year,
            int quarter,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}
