package com.skipers.skipa.domain.dashboard.application;

import com.skipers.skipa.domain.dashboard.dto.response.BusinessDashboardResponse;
import com.skipers.skipa.domain.dashboard.dto.response.LegalDashboardResponse;
import com.skipers.skipa.domain.dashboard.dto.response.ReviewCycleSummary;
import com.skipers.skipa.domain.patent.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatusType;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import com.skipers.skipa.domain.review.exception.ReviewException;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int RECENT_LIMIT = 5;

    private final ReviewCycleRepository reviewCycleRepository;
    private final ReviewRepository reviewRepository;
    private final PatentRepository patentRepository;
    private final PatentLegalStatusRepository patentLegalStatusRepository;

    public LegalDashboardResponse getLegalDashboard(Long reviewCycleId) {
        LocalDate today = LocalDate.now();
        ReviewCycle reviewCycle = resolveReviewCycle(reviewCycleId, today);
        List<Review> reviews = reviewRepository.findAllByReviewCycleId(reviewCycle.getId());
        List<Patent> patents = patentRepository.findAll();
        Set<Long> reviewedPatentIds = reviews.stream()
                .map(review -> review.getPatent().getId())
                .collect(Collectors.toSet());

        long reviewing = countPendingNotOverdue(reviews, today);
        long decided = countSubmitted(reviews);
        long overdue = countOverdue(reviews, today);
        long unread = reviews.stream()
                .filter(review -> review.getStatus() == ReviewStatus.SUBMITTED)
                .filter(review -> !review.isChecked())
                .count();
        long unrequested = patents.stream()
                .filter(patent -> !reviewedPatentIds.contains(patent.getId()))
                .count();

        return new LegalDashboardResponse(
                ReviewCycleSummary.from(reviewCycle),
                new LegalDashboardResponse.Kpi(reviews.size(), reviewing, decided, overdue, unread, unrequested),
                departmentProgress(reviews, today),
                nameCounts(patents.stream()
                        .collect(Collectors.groupingBy(patent -> normalizeGroupName(patent.getTechField()), Collectors.counting()))),
                expiryQuarterCounts(patents, today),
                recentReplies(reviews)
        );
    }

    public BusinessDashboardResponse getBusinessDashboard(User user, Long reviewCycleId) {
        if (user.getDepartment() == null) {
            throw new ReviewException(ErrorCode.FORBIDDEN);
        }

        LocalDate today = LocalDate.now();
        ReviewCycle reviewCycle = resolveReviewCycle(reviewCycleId, today);
        Long departmentId = user.getDepartment().getId();
        List<Review> reviews = reviewRepository.findAllByReviewCycleId(reviewCycle.getId()).stream()
                .filter(review -> review.getDepartment().getId().equals(departmentId))
                .filter(review -> review.getPatent().getCurrentDepartment() != null)
                .filter(review -> review.getPatent().getCurrentDepartment().getId().equals(departmentId))
                .filter(review -> review.getStatus() != ReviewStatus.SCHEDULED)
                .toList();
        List<Patent> departmentPatents = patentRepository.findByCurrentDepartmentId(
                departmentId,
                org.springframework.data.domain.Pageable.unpaged()
        ).getContent();
        List<PatentLegalStatus> legalStatuses = patentLegalStatusRepository.findAll();
        Map<Long, PatentLegalStatusType> latestLegalStatuses = latestLegalStatuses(legalStatuses);

        long submitted = countSubmitted(reviews);
        long notSubmitted = reviews.stream()
                .filter(review -> review.getStatus() != ReviewStatus.SUBMITTED)
                .count();

        return new BusinessDashboardResponse(
                ReviewCycleSummary.from(reviewCycle),
                new BusinessDashboardResponse.Kpi(reviews.size(), submitted, notSubmitted),
                pendingPatents(reviews),
                recentSubmissions(reviews),
                patentStatusSummary(departmentPatents, latestLegalStatuses, today),
                yearlyTrends(departmentPatents, legalStatuses)
        );
    }

    private ReviewCycle resolveReviewCycle(Long reviewCycleId, LocalDate today) {
        if (reviewCycleId != null) {
            return reviewCycleRepository.findById(reviewCycleId)
                    .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_CYCLE_NOT_FOUND));
        }

        return reviewCycleRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(today, today)
                .orElseThrow(() -> new ReviewException(ErrorCode.ACTIVE_REVIEW_CYCLE_NOT_FOUND));
    }

    private List<LegalDashboardResponse.DepartmentProgress> departmentProgress(List<Review> reviews, LocalDate today) {
        Map<Long, DepartmentAccumulator> byDepartment = new HashMap<>();
        for (Review review : reviews) {
            DepartmentAccumulator accumulator = byDepartment.computeIfAbsent(
                    review.getDepartment().getId(),
                    ignored -> new DepartmentAccumulator(review.getDepartment().getId(), review.getDepartment().getName())
            );
            accumulator.assigned++;
            if (review.getStatus() == ReviewStatus.SUBMITTED) {
                accumulator.decided++;
            }
        }

        return byDepartment.values().stream()
                .sorted(Comparator.comparing(DepartmentAccumulator::departmentName))
                .map(accumulator -> new LegalDashboardResponse.DepartmentProgress(
                        accumulator.departmentId(),
                        accumulator.departmentName(),
                        accumulator.assigned,
                        accumulator.decided
                ))
                .toList();
    }

    private List<LegalDashboardResponse.RecentReply> recentReplies(List<Review> reviews) {
        return reviews.stream()
                .filter(review -> review.getStatus() == ReviewStatus.SUBMITTED)
                .filter(review -> !review.isChecked())
                .filter(review -> review.getSubmittedAt() != null)
                .sorted(Comparator.comparing(Review::getSubmittedAt).reversed())
                .limit(RECENT_LIMIT)
                .map(review -> new LegalDashboardResponse.RecentReply(
                        review.getId(),
                        review.getPatent().getId(),
                        review.getPatent().getTitle(),
                        review.getPatent().getApplicationNumber(),
                        review.getDepartment().getId(),
                        review.getDepartment().getName(),
                        review.getOpinion() == null ? null : review.getOpinion().name(),
                        review.getSubmittedAt(),
                        review.isChecked(),
                        review.getDueDate()
                ))
                .toList();
    }

    private List<BusinessDashboardResponse.PatentReviewItem> pendingPatents(List<Review> reviews) {
        return reviews.stream()
                .filter(review -> review.getStatus() != ReviewStatus.SUBMITTED)
                .sorted(Comparator.comparing(Review::getDueDate).thenComparing(Review::getId))
                .limit(RECENT_LIMIT)
                .map(review -> new BusinessDashboardResponse.PatentReviewItem(
                        review.getId(),
                        review.getPatent().getId(),
                        review.getPatent().getTitle(),
                        review.getPatent().getApplicationNumber(),
                        review.getStatus().name()
                ))
                .toList();
    }

    private List<BusinessDashboardResponse.SubmissionItem> recentSubmissions(List<Review> reviews) {
        return reviews.stream()
                .filter(review -> review.getStatus() == ReviewStatus.SUBMITTED)
                .filter(review -> review.getSubmittedAt() != null)
                .sorted(Comparator.comparing(Review::getSubmittedAt).reversed())
                .limit(RECENT_LIMIT)
                .map(review -> new BusinessDashboardResponse.SubmissionItem(
                        review.getId(),
                        review.getPatent().getId(),
                        review.getPatent().getTitle(),
                        review.getPatent().getApplicationNumber(),
                        review.getOpinion() == null ? null : review.getOpinion().name(),
                        review.getSubmittedAt()
                ))
                .toList();
    }

    private BusinessDashboardResponse.PatentStatusSummary patentStatusSummary(
            List<Patent> patents,
            Map<Long, PatentLegalStatusType> latestLegalStatuses,
            LocalDate today
    ) {
        long active = 0;
        long inactive = 0;

        for (Patent patent : patents) {
            if (isInactive(patent, latestLegalStatuses.get(patent.getId()), today)) {
                inactive++;
            } else {
                active++;
            }
        }

        return new BusinessDashboardResponse.PatentStatusSummary(active, inactive);
    }

    private List<BusinessDashboardResponse.YearlyTrend> yearlyTrends(
            List<Patent> patents,
            List<PatentLegalStatus> legalStatuses
    ) {
        Map<Integer, YearAccumulator> byYear = new LinkedHashMap<>();
        Set<Long> patentIds = patents.stream()
                .map(Patent::getId)
                .collect(Collectors.toSet());

        patents.stream()
                .map(Patent::getApplicationDate)
                .filter(applicationDate -> applicationDate != null)
                .map(LocalDate::getYear)
                .sorted()
                .forEach(year -> byYear.computeIfAbsent(year, YearAccumulator::new).applications++);
        legalStatuses.stream()
                .filter(legalStatus -> patentIds.contains(legalStatus.getPatent().getId()))
                .filter(legalStatus -> isAbandonedOrExpired(legalStatus.getStatus()))
                .map(PatentLegalStatus::getChangedAt)
                .filter(changedAt -> changedAt != null)
                .map(LocalDate::getYear)
                .sorted()
                .forEach(year -> byYear.computeIfAbsent(year, YearAccumulator::new).expiredOrAbandoned++);

        return byYear.values().stream()
                .map(accumulator -> new BusinessDashboardResponse.YearlyTrend(
                        accumulator.year,
                        accumulator.applications,
                        accumulator.expiredOrAbandoned
                ))
                .toList();
    }

    private Map<Long, PatentLegalStatusType> latestLegalStatuses(List<PatentLegalStatus> legalStatuses) {
        Map<Long, PatentLegalStatus> latestByPatentId = new HashMap<>();
        for (PatentLegalStatus legalStatus : legalStatuses) {
            Long patentId = legalStatus.getPatent().getId();
            PatentLegalStatus current = latestByPatentId.get(patentId);
            if (current == null || compareLegalStatusRecency(legalStatus, current) > 0) {
                latestByPatentId.put(patentId, legalStatus);
            }
        }

        Map<Long, PatentLegalStatusType> result = new HashMap<>();
        latestByPatentId.forEach((patentId, legalStatus) -> result.put(patentId, legalStatus.getStatus()));
        return result;
    }

    private int compareLegalStatusRecency(PatentLegalStatus left, PatentLegalStatus right) {
        LocalDate leftChangedAt = left.getChangedAt();
        LocalDate rightChangedAt = right.getChangedAt();
        if (leftChangedAt == null && rightChangedAt == null) {
            return left.getId().compareTo(right.getId());
        }
        if (leftChangedAt == null) {
            return -1;
        }
        if (rightChangedAt == null) {
            return 1;
        }

        int dateComparison = leftChangedAt.compareTo(rightChangedAt);
        return dateComparison != 0 ? dateComparison : left.getId().compareTo(right.getId());
    }

    private List<LegalDashboardResponse.QuarterCount> expiryQuarterCounts(List<Patent> patents, LocalDate today) {
        Map<String, Long> counts = currentExpiryQuarters(today);
        for (Patent patent : patents) {
            String quarter = expiryQuarter(patent.getExpiryDate(), today);
            if (quarter != null) {
                counts.merge(quarter, 1L, Long::sum);
            }
        }

        return counts.entrySet().stream()
                .map(entry -> new LegalDashboardResponse.QuarterCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<String, Long> currentExpiryQuarters(LocalDate today) {
        Map<String, Long> quarters = new LinkedHashMap<>();
        YearMonth current = YearMonth.from(today);
        int currentQuarterStartMonth = ((current.getMonthValue() - 1) / 3) * 3 + 1;
        YearMonth quarterStart = YearMonth.of(current.getYear(), currentQuarterStartMonth);
        for (int i = 0; i < 4; i++) {
            YearMonth quarter = quarterStart.plusMonths(i * 3L);
            quarters.put(quarter.getYear() + "Q" + (((quarter.getMonthValue() - 1) / 3) + 1), 0L);
        }
        return quarters;
    }

    private String expiryQuarter(LocalDate expiryDate, LocalDate today) {
        if (expiryDate == null || expiryDate.isBefore(today)) {
            return null;
        }

        YearMonth current = YearMonth.from(today);
        int currentQuarterStartMonth = ((current.getMonthValue() - 1) / 3) * 3 + 1;
        YearMonth quarterStart = YearMonth.of(current.getYear(), currentQuarterStartMonth);
        YearMonth expiryMonth = YearMonth.from(expiryDate);
        if (expiryMonth.isBefore(quarterStart) || !expiryMonth.isBefore(quarterStart.plusMonths(12))) {
            return null;
        }

        return expiryMonth.getYear() + "Q" + (((expiryMonth.getMonthValue() - 1) / 3) + 1);
    }

    private List<LegalDashboardResponse.NameCount> nameCounts(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new LegalDashboardResponse.NameCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private long countSubmitted(List<Review> reviews) {
        return reviews.stream()
                .filter(review -> review.getStatus() == ReviewStatus.SUBMITTED)
                .count();
    }

    private long countPendingNotOverdue(List<Review> reviews, LocalDate today) {
        return reviews.stream()
                .filter(review -> review.getStatus() == ReviewStatus.PENDING)
                .filter(review -> !review.getDueDate().isBefore(today))
                .count();
    }

    private long countOverdue(List<Review> reviews, LocalDate today) {
        return reviews.stream()
                .filter(review -> review.getStatus() == ReviewStatus.OVERDUE
                        || review.getStatus() == ReviewStatus.PENDING && review.getDueDate().isBefore(today))
                .count();
    }

    private String normalizeGroupName(String value) {
        return value == null || value.isBlank() ? "미분류" : value;
    }

    private boolean isInactive(Patent patent, PatentLegalStatusType status, LocalDate today) {
        LocalDate expiryDate = patent.getExpiryDate();
        return isAbandonedOrExpired(status) || (expiryDate != null && expiryDate.isBefore(today));
    }

    private boolean isAbandonedOrExpired(PatentLegalStatusType status) {
        return EnumSet.of(
                PatentLegalStatusType.ABANDONED,
                PatentLegalStatusType.EXPIRED,
                PatentLegalStatusType.INVALIDATED,
                PatentLegalStatusType.WITHDRAWN,
                PatentLegalStatusType.REJECTED
        ).contains(status);
    }

    private static class DepartmentAccumulator {
        private final Long departmentId;
        private final String departmentName;
        private long assigned;
        private long decided;

        private DepartmentAccumulator(Long departmentId, String departmentName) {
            this.departmentId = departmentId;
            this.departmentName = departmentName;
        }

        private Long departmentId() {
            return departmentId;
        }

        private String departmentName() {
            return departmentName;
        }
    }

    private static class YearAccumulator {
        private final int year;
        private long applications;
        private long expiredOrAbandoned;

        private YearAccumulator(int year) {
            this.year = year;
        }
    }
}
