package com.skipers.skipa.domain.portfolio.application;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentAnnuity;
import com.skipers.skipa.domain.patent.domain.PatentAnnuityStatus;
import com.skipers.skipa.domain.portfolio.dto.response.PortfolioDecisionResponse;
import com.skipers.skipa.domain.portfolio.dto.response.PortfolioDistributionResponse;
import com.skipers.skipa.domain.portfolio.dto.response.PortfolioTrendsResponse;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.domain.ReportStatus;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.BusinessOpinion;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private static final int TREND_YEAR_COUNT = 7;

    private final PatentRepository patentRepository;
    private final PatentAnnuityRepository patentAnnuityRepository;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;

    public PortfolioDistributionResponse getDistribution() {
        List<Patent> patents = patentRepository.findAll();
        Map<String, Long> byTechField = new HashMap<>();
        Map<String, Long> byFilingCountry = new HashMap<>();
        Map<Long, DepartmentAccumulator> byDepartment = new HashMap<>();
        Map<Long, Report> latestCompletedReportsByPatentId = latestCompletedReportsByPatentId();
        GradeAccumulator totalGrade = new GradeAccumulator(null, "전체");
        Map<Long, GradeAccumulator> byDepartmentGrade = new HashMap<>();

        for (Patent patent : patents) {
            byTechField.merge(normalizeGroupName(patent.getTechField()), 1L, Long::sum);
            byFilingCountry.merge(normalizeGroupName(patent.getFilingCountry()), 1L, Long::sum);
            Department department = patent.getCurrentDepartment();
            Long departmentId = department == null ? null : department.getId();
            String departmentName = department == null ? "미배정" : department.getName();
            DepartmentAccumulator accumulator = byDepartment.computeIfAbsent(
                    departmentId,
                    ignored -> new DepartmentAccumulator(departmentId, departmentName)
            );
            accumulator.count++;

            Report report = latestCompletedReportsByPatentId.get(patent.getId());
            if (report != null && report.getValueGrade() != null) {
                totalGrade.accumulate(report.getValueGrade());
                byDepartmentGrade.computeIfAbsent(
                        departmentId,
                        ignored -> new GradeAccumulator(departmentId, departmentName)
                ).accumulate(report.getValueGrade());
            }
        }

        return new PortfolioDistributionResponse(
                gradeDistributions(totalGrade, byDepartmentGrade),
                nameCounts(byTechField),
                countryCounts(byFilingCountry),
                departmentCounts(byDepartment)
        );
    }

    public PortfolioTrendsResponse getTrends() {
        int currentYear = LocalDate.now().getYear();
        int startYear = currentYear - TREND_YEAR_COUNT + 1;
        List<Patent> patents = patentRepository.findAll();
        List<PatentAnnuity> annuities = patentAnnuityRepository.findAll();
        Map<Integer, YearPatentAccumulator> patentTrends = patentTrendYears(startYear, currentYear);
        Map<Integer, Long> annuityCosts = annuityCostYears(startYear, currentYear);

        patents.stream()
                .map(Patent::getApplicationDate)
                .filter(date -> date != null)
                .map(LocalDate::getYear)
                .filter(year -> isTrendYear(year, startYear, currentYear))
                .forEach(year -> patentTrends.computeIfAbsent(year, YearPatentAccumulator::new).applications++);
        patents.stream()
                .map(Patent::getRegistrationDate)
                .filter(date -> date != null)
                .map(LocalDate::getYear)
                .filter(year -> isTrendYear(year, startYear, currentYear))
                .forEach(year -> patentTrends.computeIfAbsent(year, YearPatentAccumulator::new).registrations++);
        patents.stream()
                .map(Patent::getExpiryDate)
                .filter(date -> date != null)
                .map(LocalDate::getYear)
                .filter(year -> isTrendYear(year, startYear, currentYear))
                .forEach(year -> patentTrends.computeIfAbsent(year, YearPatentAccumulator::new).expiries++);

        annuities.stream()
                .filter(annuity -> annuity.getStatus() == PatentAnnuityStatus.PAID)
                .filter(annuity -> annuity.getAmount() != null)
                .filter(annuity -> annuity.getPaidDate() != null)
                .forEach(annuity -> {
                    int year = annuity.getPaidDate().getYear();
                    if (isTrendYear(year, startYear, currentYear)) {
                        annuityCosts.merge(year, annuity.getAmount().longValue(), Long::sum);
                    }
                });

        return new PortfolioTrendsResponse(
                patentTrends.values().stream()
                        .map(accumulator -> new PortfolioTrendsResponse.YearlyPatentTrend(
                                accumulator.year,
                                accumulator.applications,
                                accumulator.registrations,
                                accumulator.expiries
                        ))
                        .toList(),
                annuityCosts.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> new PortfolioTrendsResponse.YearlyAnnuityCost(entry.getKey(), entry.getValue()))
                        .toList()
        );
    }

    private Map<Integer, YearPatentAccumulator> patentTrendYears(int startYear, int currentYear) {
        Map<Integer, YearPatentAccumulator> trends = new LinkedHashMap<>();
        for (int year = startYear; year <= currentYear; year++) {
            trends.put(year, new YearPatentAccumulator(year));
        }
        return trends;
    }

    private Map<Integer, Long> annuityCostYears(int startYear, int currentYear) {
        Map<Integer, Long> costs = new LinkedHashMap<>();
        for (int year = startYear; year <= currentYear; year++) {
            costs.put(year, 0L);
        }
        return costs;
    }

    private boolean isTrendYear(int year, int startYear, int currentYear) {
        return year >= startYear && year <= currentYear;
    }

    public PortfolioDecisionResponse getDecisions() {
        List<Review> submittedReviews = reviewRepository.findAll().stream()
                .filter(review -> review.getStatus() == ReviewStatus.SUBMITTED)
                .filter(review -> review.getOpinion() != null)
                .toList();
        Map<String, DecisionAccumulator> byQuarter = new LinkedHashMap<>();
        Map<Long, DepartmentDecisionAccumulator> byDepartment = new HashMap<>();
        Map<String, DecisionAccumulator> byTechField = new HashMap<>();

        for (Review review : submittedReviews) {
            String quarter = submittedQuarter(review);
            if (quarter != null) {
                accumulate(byQuarter.computeIfAbsent(quarter, ignored -> new DecisionAccumulator()), review.getOpinion());
            }

            DepartmentDecisionAccumulator departmentAccumulator = byDepartment.computeIfAbsent(
                    review.getDepartment().getId(),
                    ignored -> new DepartmentDecisionAccumulator(review.getDepartment().getId(), review.getDepartment().getName())
            );
            accumulate(departmentAccumulator, review.getOpinion());
            accumulate(
                    byTechField.computeIfAbsent(normalizeGroupName(review.getPatent().getTechField()), ignored -> new DecisionAccumulator()),
                    review.getOpinion()
            );
        }

        return new PortfolioDecisionResponse(
                quarterDecisions(byQuarter),
                departmentDecisions(byDepartment),
                techFieldDecisions(byTechField)
        );
    }

    private String submittedQuarter(Review review) {
        if (review.getSubmittedAt() == null) {
            return null;
        }

        YearMonth submittedMonth = YearMonth.from(review.getSubmittedAt().atZone(java.time.ZoneId.systemDefault()));
        return submittedMonth.getYear() + "Q" + (((submittedMonth.getMonthValue() - 1) / 3) + 1);
    }

    private void accumulate(DecisionAccumulator accumulator, BusinessOpinion opinion) {
        if (opinion == BusinessOpinion.MAINTAIN) {
            accumulator.maintain++;
        } else if (opinion == BusinessOpinion.ABANDON) {
            accumulator.abandon++;
        }
    }

    private List<PortfolioDistributionResponse.NameCount> nameCounts(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new PortfolioDistributionResponse.NameCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<PortfolioDistributionResponse.CountryCount> countryCounts(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new PortfolioDistributionResponse.CountryCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<PortfolioDistributionResponse.DepartmentCount> departmentCounts(
            Map<Long, DepartmentAccumulator> counts
    ) {
        return counts.values().stream()
                .sorted(Comparator.comparing(DepartmentAccumulator::departmentName))
                .map(accumulator -> new PortfolioDistributionResponse.DepartmentCount(
                        accumulator.departmentId(),
                        accumulator.departmentName(),
                        accumulator.count
                ))
                .toList();
    }

    private Map<Long, Report> latestCompletedReportsByPatentId() {
        Map<Long, Report> reportsByPatentId = new HashMap<>();
        reportRepository.findAllByStatusIn(List.of(ReportStatus.REPORT_COMPLETED, ReportStatus.EMBEDDING_COMPLETED)).stream()
                .sorted(Comparator.comparing(Report::getId).reversed())
                .forEach(report -> reportsByPatentId.putIfAbsent(report.getPatent().getId(), report));
        return reportsByPatentId;
    }

    private List<PortfolioDistributionResponse.GradeDistribution> gradeDistributions(
            GradeAccumulator total,
            Map<Long, GradeAccumulator> byDepartment
    ) {
        List<PortfolioDistributionResponse.GradeDistribution> distributions = new ArrayList<>();
        distributions.add(total.toResponse());
        byDepartment.values().stream()
                .sorted(Comparator.comparing(GradeAccumulator::departmentName))
                .map(GradeAccumulator::toResponse)
                .forEach(distributions::add);
        return distributions;
    }

    private List<PortfolioDecisionResponse.QuarterDecision> quarterDecisions(Map<String, DecisionAccumulator> decisions) {
        return decisions.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new PortfolioDecisionResponse.QuarterDecision(
                        entry.getKey(),
                        entry.getValue().maintain,
                        entry.getValue().abandon
                ))
                .toList();
    }

    private List<PortfolioDecisionResponse.DepartmentDecision> departmentDecisions(
            Map<Long, DepartmentDecisionAccumulator> decisions
    ) {
        return decisions.values().stream()
                .sorted(Comparator.comparing(DepartmentDecisionAccumulator::departmentName))
                .map(accumulator -> new PortfolioDecisionResponse.DepartmentDecision(
                        accumulator.departmentId(),
                        accumulator.departmentName(),
                        accumulator.maintain,
                        accumulator.abandon
                ))
                .toList();
    }

    private List<PortfolioDecisionResponse.TechFieldDecision> techFieldDecisions(
            Map<String, DecisionAccumulator> decisions
    ) {
        return decisions.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new PortfolioDecisionResponse.TechFieldDecision(
                        entry.getKey(),
                        entry.getValue().maintain,
                        entry.getValue().abandon
                ))
                .toList();
    }

    private String normalizeGroupName(String value) {
        return value == null || value.isBlank() ? "미분류" : value;
    }

    private static class DepartmentAccumulator {
        private final Long departmentId;
        private final String departmentName;
        private long count;

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

    private static class GradeAccumulator {
        private final Long departmentId;
        private final String departmentName;
        private long s;
        private long a;
        private long b;
        private long c;
        private long d;

        private GradeAccumulator(Long departmentId, String departmentName) {
            this.departmentId = departmentId;
            this.departmentName = departmentName;
        }

        private void accumulate(String grade) {
            switch (grade) {
                case "S" -> s++;
                case "A" -> a++;
                case "B" -> b++;
                case "C" -> c++;
                case "D" -> d++;
                default -> {
                }
            }
        }

        private String departmentName() {
            return departmentName;
        }

        private PortfolioDistributionResponse.GradeDistribution toResponse() {
            return new PortfolioDistributionResponse.GradeDistribution(
                    departmentId,
                    departmentName,
                    s,
                    a,
                    b,
                    c,
                    d
            );
        }
    }

    private static class YearPatentAccumulator {
        private final int year;
        private long applications;
        private long registrations;
        private long expiries;

        private YearPatentAccumulator(int year) {
            this.year = year;
        }
    }

    private static class DecisionAccumulator {
        protected long maintain;
        protected long abandon;
    }

    private static class DepartmentDecisionAccumulator extends DecisionAccumulator {
        private final Long departmentId;
        private final String departmentName;

        private DepartmentDecisionAccumulator(Long departmentId, String departmentName) {
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
}
