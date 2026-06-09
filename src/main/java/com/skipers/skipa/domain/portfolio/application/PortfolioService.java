package com.skipers.skipa.domain.portfolio.application;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentAnnuity;
import com.skipers.skipa.domain.portfolio.dto.response.PortfolioDecisionResponse;
import com.skipers.skipa.domain.portfolio.dto.response.PortfolioDistributionResponse;
import com.skipers.skipa.domain.portfolio.dto.response.PortfolioSummaryResponse;
import com.skipers.skipa.domain.portfolio.dto.response.PortfolioTrendsResponse;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PatentRepository patentRepository;
    private final PatentAnnuityRepository patentAnnuityRepository;
    private final ReviewRepository reviewRepository;

    public PortfolioSummaryResponse getSummary() {
        LocalDate today = LocalDate.now();
        List<Patent> patents = patentRepository.findAll();
        long expiringWithinYear = patents.stream()
                .map(Patent::getExpiryDate)
                .filter(expiryDate -> expiryDate != null)
                .filter(expiryDate -> !expiryDate.isBefore(today))
                .filter(expiryDate -> !expiryDate.isAfter(today.plusYears(1)))
                .count();
        Set<String> countries = distinctNormalized(patents.stream().map(Patent::getFilingCountry).toList());
        Set<String> techFields = distinctNormalized(patents.stream().map(Patent::getTechField).toList());

        return new PortfolioSummaryResponse(
                patents.size(),
                expiringWithinYear,
                countries.size(),
                techFields.size(),
                insights(patents.size(), expiringWithinYear, countries.size(), techFields.size())
        );
    }

    public PortfolioDistributionResponse getDistribution() {
        List<Patent> patents = patentRepository.findAll();
        Map<String, Long> byTechField = new HashMap<>();
        Map<String, Long> byFilingCountry = new HashMap<>();
        Map<Long, DepartmentAccumulator> byDepartment = new HashMap<>();

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
        }

        return new PortfolioDistributionResponse(
                List.of(),
                nameCounts(byTechField),
                countryCounts(byFilingCountry),
                departmentCounts(byDepartment)
        );
    }

    public PortfolioTrendsResponse getTrends() {
        List<Patent> patents = patentRepository.findAll();
        List<PatentAnnuity> annuities = patentAnnuityRepository.findAll();
        Map<Integer, YearPatentAccumulator> patentTrends = new LinkedHashMap<>();
        Map<Integer, Long> annuityCosts = new LinkedHashMap<>();

        patents.stream()
                .map(Patent::getApplicationDate)
                .filter(date -> date != null)
                .map(LocalDate::getYear)
                .sorted()
                .forEach(year -> patentTrends.computeIfAbsent(year, YearPatentAccumulator::new).applications++);
        patents.stream()
                .map(Patent::getRegistrationDate)
                .filter(date -> date != null)
                .map(LocalDate::getYear)
                .sorted()
                .forEach(year -> patentTrends.computeIfAbsent(year, YearPatentAccumulator::new).registrations++);
        patents.stream()
                .map(Patent::getExpiryDate)
                .filter(date -> date != null)
                .map(LocalDate::getYear)
                .sorted()
                .forEach(year -> patentTrends.computeIfAbsent(year, YearPatentAccumulator::new).expiries++);

        annuities.stream()
                .filter(annuity -> annuity.getAmount() != null)
                .forEach(annuity -> {
                    LocalDate baseDate = annuity.getPaidDate() != null ? annuity.getPaidDate() : annuity.getDueDate();
                    if (baseDate != null) {
                        annuityCosts.merge(baseDate.getYear(), annuity.getAmount().longValue(), Long::sum);
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

    private List<String> insights(long total, long expiringWithinYear, long countryCount, long techFieldCount) {
        List<String> insights = new ArrayList<>();
        insights.add("전체 보유 특허는 " + total + "건입니다.");
        if (expiringWithinYear > 0) {
            insights.add("1년 내 만료 예정 특허 " + expiringWithinYear + "건에 대한 재평가 우선순위 검토가 필요합니다.");
        }
        if (countryCount > 1 || techFieldCount > 1) {
            insights.add("국가 및 기술 분야별 분포를 기준으로 포트폴리오 편중 여부를 확인할 수 있습니다.");
        }
        return insights;
    }

    private Set<String> distinctNormalized(List<String> values) {
        return values.stream()
                .map(this::normalizeGroupName)
                .collect(Collectors.toCollection(HashSet::new));
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
