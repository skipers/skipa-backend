package com.skipers.skipa.domain.dashboard.application;

import com.skipers.skipa.domain.dashboard.dto.response.DashboardAssignmentResponse;
import com.skipers.skipa.domain.dashboard.dto.response.DashboardDepartmentsResponse;
import com.skipers.skipa.domain.dashboard.dto.response.DashboardDistributionResponse;
import com.skipers.skipa.domain.dashboard.dto.response.DashboardSummaryResponse;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final PatentRepository patentRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewCycleRepository reviewCycleRepository;

    public DashboardSummaryResponse getSummary() {
        return findActiveReviewCycle()
                .map(this::buildSummary)
                .orElseGet(() -> new DashboardSummaryResponse(
                        0,
                        0,
                        new DashboardSummaryResponse.Kpi(0, 0, 0)
                ));
    }

    public DashboardAssignmentResponse getAssignment() {
        long unassigned = patentRepository.countByCurrentDepartmentIsNull();
        long assigned = patentRepository.countByCurrentDepartmentIsNotNull();
        long completed = findActiveReviewCycle()
                .map(reviewCycle -> reviewRepository.countByReviewCycleIdAndStatus(reviewCycle.getId(), ReviewStatus.SUBMITTED))
                .orElse(0L);

        return new DashboardAssignmentResponse(unassigned, assigned, completed);
    }

    public DashboardDistributionResponse getDistribution() {
        List<DashboardDistributionResponse.TechFieldItem> byTechField = patentRepository.findByTechFieldIsNotNull().stream()
                .map(Patent::getTechField)
                .filter(this::hasText)
                .collect(java.util.stream.Collectors.groupingBy(
                        String::trim,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new DashboardDistributionResponse.TechFieldItem(entry.getKey(), entry.getValue()))
                .toList();

        List<DashboardDistributionResponse.ExpiryQuarterItem> byExpiryQuarter = patentRepository
                .findByExpiryDateIsNotNullAndExpiryDateGreaterThanEqual(LocalDate.now()).stream()
                .map(Patent::getExpiryDate)
                .collect(java.util.stream.Collectors.groupingBy(
                        this::toQuarterLabel,
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> new DashboardDistributionResponse.ExpiryQuarterItem(entry.getKey(), entry.getValue()))
                .toList();

        return new DashboardDistributionResponse(byTechField, byExpiryQuarter);
    }

    public DashboardDepartmentsResponse getDepartments() {
        return findActiveReviewCycle()
                .map(reviewCycle -> {
                    Map<Long, DepartmentStats> departmentStats = new LinkedHashMap<>();

                    reviewRepository.findAllByReviewCycleId(reviewCycle.getId()).forEach(review -> {
                        Long departmentId = review.getDepartment().getId();
                        DepartmentStats stats = departmentStats.computeIfAbsent(
                                departmentId,
                                ignored -> new DepartmentStats(departmentId, review.getDepartment().getName())
                        );
                        stats.assigned += 1;
                        if (review.getStatus() == ReviewStatus.PENDING) {
                            stats.reviewing += 1;
                        }
                        if (review.getStatus() == ReviewStatus.SUBMITTED) {
                            stats.decided += 1;
                        }
                    });

                    List<DashboardDepartmentsResponse.Item> items = departmentStats.values().stream()
                            .map(DepartmentStats::toItem)
                            .sorted(Comparator.comparing(DashboardDepartmentsResponse.Item::departmentName))
                            .toList();

                    return new DashboardDepartmentsResponse(items);
                })
                .orElseGet(() -> new DashboardDepartmentsResponse(List.of()));
    }

    private DashboardSummaryResponse buildSummary(ReviewCycle reviewCycle) {
        long requested = reviewRepository.countByReviewCycleId(reviewCycle.getId());
        long reviewing = reviewRepository.countByReviewCycleIdAndStatus(reviewCycle.getId(), ReviewStatus.PENDING);
        long decided = reviewRepository.countByReviewCycleIdAndStatus(reviewCycle.getId(), ReviewStatus.SUBMITTED);
        long delayed = reviewRepository.countByReviewCycleIdAndStatusAndDueDateBefore(
                reviewCycle.getId(),
                ReviewStatus.PENDING,
                LocalDate.now()
        );

        return new DashboardSummaryResponse(
                calculateProgressRate(requested, decided),
                delayed,
                new DashboardSummaryResponse.Kpi(requested, reviewing, decided)
        );
    }

    private java.util.Optional<ReviewCycle> findActiveReviewCycle() {
        LocalDate today = LocalDate.now();
        return reviewCycleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(today, today);
    }

    private int calculateProgressRate(long requested, long decided) {
        if (requested == 0) {
            return 0;
        }
        return (int) Math.round((double) decided * 100 / requested);
    }

    private String toQuarterLabel(LocalDate date) {
        int quarter = ((date.getMonthValue() - 1) / 3) + 1;
        return date.getYear() + "Q" + quarter;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static class DepartmentStats {
        private final Long departmentId;
        private final String departmentName;
        private long assigned;
        private long reviewing;
        private long decided;

        private DepartmentStats(Long departmentId, String departmentName) {
            this.departmentId = departmentId;
            this.departmentName = departmentName;
        }

        private DashboardDepartmentsResponse.Item toItem() {
            return new DashboardDepartmentsResponse.Item(
                    departmentId,
                    departmentName,
                    assigned,
                    reviewing,
                    decided,
                    assigned == 0 ? 0 : (int) Math.round((double) decided * 100 / assigned)
            );
        }
    }
}
