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
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpiringPatentService {

    private static final List<Integer> SUMMARY_PERIOD_MONTHS = List.of(3, 6, 12, 36, 60);
    private static final int DEFAULT_PERIOD_MONTHS = 12;

    private final PatentRepository patentRepository;

    public ExpiringPatentSummaryResponse getSummary(User user, Integer periodMonths) {
        LocalDate today = LocalDate.now();
        List<Patent> patents = scopedPatents(user);
        int selectedPeriodMonths = normalizePeriodMonths(periodMonths);
        LocalDate selectedEndDate = today.plusMonths(selectedPeriodMonths);

        return new ExpiringPatentSummaryResponse(
                SUMMARY_PERIOD_MONTHS.stream()
                        .map(months -> new ExpiringPatentSummaryResponse.PeriodCount(
                                months,
                                countExpiringUntil(patents, today, today.plusMonths(months))
                        ))
                        .toList(),
                techFieldCounts(expiringPatents(patents, today, selectedEndDate))
        );
    }

    public Page<ExpiringPatentItemResponse> getAll(User user, Integer periodMonths, Pageable pageable) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusMonths(normalizePeriodMonths(periodMonths));
        List<ExpiringPatentItemResponse> responses = expiringPatents(scopedPatents(user), today, endDate).stream()
                .sorted(Comparator.comparing(Patent::getExpiryDate).thenComparing(Patent::getId))
                .map(patent -> ExpiringPatentItemResponse.from(patent, today))
                .toList();

        int start = Math.min((int) pageable.getOffset(), responses.size());
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        return new PageImpl<>(responses.subList(start, end), pageable, responses.size());
    }

    public ExpiringPatentCalendarResponse getCalendar(User user, Integer periodMonths) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusMonths(normalizePeriodMonths(periodMonths));
        Map<YearMonth, List<ExpiringPatentItemResponse>> byMonth = new LinkedHashMap<>();

        expiringPatents(scopedPatents(user), today, endDate).stream()
                .sorted(Comparator.comparing(Patent::getExpiryDate).thenComparing(Patent::getId))
                .forEach(patent -> byMonth.computeIfAbsent(YearMonth.from(patent.getExpiryDate()), ignored -> new java.util.ArrayList<>())
                        .add(ExpiringPatentItemResponse.from(patent, today)));

        return new ExpiringPatentCalendarResponse(byMonth.entrySet().stream()
                .map(entry -> new ExpiringPatentCalendarResponse.MonthBucket(
                        entry.getKey().getYear(),
                        entry.getKey().getMonthValue(),
                        entry.getValue().size(),
                        entry.getValue()
                ))
                .toList());
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
                .filter(patent -> !patent.getExpiryDate().isAfter(endDate))
                .toList();
    }

    private long countExpiringUntil(List<Patent> patents, LocalDate today, LocalDate endDate) {
        return expiringPatents(patents, today, endDate).size();
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

    private int normalizePeriodMonths(Integer periodMonths) {
        if (periodMonths == null) {
            return DEFAULT_PERIOD_MONTHS;
        }
        if (!SUMMARY_PERIOD_MONTHS.contains(periodMonths)) {
            throw new ReviewException(ErrorCode.INVALID_REQUEST);
        }
        return periodMonths;
    }

    private String normalizeGroupName(String value) {
        return value == null || value.isBlank() ? "미분류" : value;
    }
}
