package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.dto.response.ExpiringPatentCalendarResponse;
import com.skipers.skipa.domain.patent.dto.response.ExpiringPatentItemResponse;
import com.skipers.skipa.domain.patent.dto.response.ExpiringPatentSummaryResponse;
import com.skipers.skipa.domain.review.exception.ReviewException;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpiringPatentService {

    private static final List<Integer> SUMMARY_PERIOD_MONTHS = List.of(3, 6, 12, 36, 60);
    private static final int LIST_PERIOD_YEARS = 5;

    private final PatentRepository patentRepository;

    public ExpiringPatentSummaryResponse getSummary(User user) {
        LocalDate today = LocalDate.now();
        List<Patent> patents = scopedPatents(user);

        return new ExpiringPatentSummaryResponse(
                summaryPeriods().stream()
                        .map(period -> new ExpiringPatentSummaryResponse.PeriodTechFieldCount(
                                period.name(),
                                period.months(),
                                techFieldCounts(expiringPatents(patents, today, period.endDate(today)))
                        ))
                        .toList()
        );
    }

    public Page<ExpiringPatentItemResponse> getAll(User user, Pageable pageable) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(LIST_PERIOD_YEARS);
        List<ExpiringPatentItemResponse> responses = expiringPatents(scopedPatents(user), today, endDate).stream()
                .sorted(Comparator.comparing(Patent::getExpiryDate).thenComparing(Patent::getId))
                .map(patent -> ExpiringPatentItemResponse.from(patent, today))
                .toList();

        int start = Math.min((int) pageable.getOffset(), responses.size());
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        return new PageImpl<>(responses.subList(start, end), pageable, responses.size());
    }

    public ExpiringPatentCalendarResponse getCalendar(User user, Integer year) {
        int selectedYear = year == null ? LocalDate.now().getYear() : year;
        LocalDate today = LocalDate.now();
        LocalDate startDate = LocalDate.of(selectedYear, 1, 1);
        LocalDate endDate = LocalDate.of(selectedYear, 12, 31);
        Map<Integer, Long> byMonth = new HashMap<>();

        scopedPatents(user).stream()
                .filter(patent -> patent.getExpiryDate() != null)
                .filter(patent -> !patent.getExpiryDate().isBefore(today))
                .filter(patent -> !patent.getExpiryDate().isBefore(startDate))
                .filter(patent -> !patent.getExpiryDate().isAfter(endDate))
                .forEach(patent -> byMonth.merge(patent.getExpiryDate().getMonthValue(), 1L, Long::sum));

        List<ExpiringPatentCalendarResponse.MonthBucket> months = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            months.add(new ExpiringPatentCalendarResponse.MonthBucket(month, byMonth.getOrDefault(month, 0L)));
        }
        return new ExpiringPatentCalendarResponse(months);
    }

    private List<Patent> scopedPatents(User user) {
        if (user.getRole() == UserRole.BUSINESS) {
            if (user.getDepartment() == null) {
                throw new ReviewException(ErrorCode.FORBIDDEN);
            }
            return patentRepository.findByCurrentDepartmentId(user.getDepartment().getId(), Pageable.unpaged()).getContent();
        }

        return patentRepository.findAll();
    }

    private List<Patent> expiringPatents(List<Patent> patents, LocalDate today, LocalDate endDate) {
        return patents.stream()
                .filter(patent -> patent.getExpiryDate() != null)
                .filter(patent -> !patent.getExpiryDate().isBefore(today))
                .filter(patent -> endDate == null || !patent.getExpiryDate().isAfter(endDate))
                .toList();
    }

    private List<ExpiringPatentSummaryResponse.TechFieldCount> techFieldCounts(List<Patent> patents) {
        Map<String, Long> counts = new HashMap<>();
        for (Patent patent : patents) {
            counts.merge(normalizeGroupName(patent.getTechField()), 1L, Long::sum);
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ExpiringPatentSummaryResponse.TechFieldCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<SummaryPeriod> summaryPeriods() {
        List<SummaryPeriod> periods = new ArrayList<>();
        periods.add(new SummaryPeriod("전체", null));
        SUMMARY_PERIOD_MONTHS.forEach(months -> periods.add(new SummaryPeriod(periodName(months), months)));
        return periods;
    }

    private String periodName(int months) {
        return switch (months) {
            case 3 -> "3개월";
            case 6 -> "6개월";
            case 12 -> "1년";
            case 36 -> "3년";
            case 60 -> "5년";
            default -> months + "개월";
        };
    }

    private String normalizeGroupName(String value) {
        return value == null || value.isBlank() ? "미분류" : value;
    }

    private record SummaryPeriod(String name, Integer months) {
        private LocalDate endDate(LocalDate today) {
            return months == null ? null : today.plusMonths(months);
        }
    }
}
