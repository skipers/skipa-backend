package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentApprovalStatus;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpiringPatentService {

    private static final List<Integer> SUMMARY_PERIOD_MONTHS = List.of(3, 6, 12, 36, 60);
    private static final int DEFAULT_LIST_PERIOD_MONTHS = 60;

    private final PatentRepository patentRepository;

    public ExpiringPatentSummaryResponse getSummary(User user) {
        LocalDate today = LocalDate.now();
        List<Patent> patents = scopedPatents(user);

        return new ExpiringPatentSummaryResponse(
                SUMMARY_PERIOD_MONTHS.stream()
                        .map(months -> new ExpiringPatentSummaryResponse.PeriodTechFieldCount(
                                months,
                                techFieldCounts(expiringPatents(patents, today, today.plusMonths(months)))
                        ))
                        .toList()
        );
    }

    public Page<ExpiringPatentItemResponse> getAll(User user, Integer months, Pageable pageable) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusMonths(normalizeListMonths(months));
        List<ExpiringPatentItemResponse> responses = expiringPatents(scopedPatents(user), today, endDate).stream()
                .sorted(Comparator.comparing(Patent::getExpiryDate).thenComparing(Patent::getId))
                .map(ExpiringPatentItemResponse::from)
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
        Map<Integer, List<Patent>> byMonth = new HashMap<>();

        scopedPatents(user).stream()
                .filter(patent -> patent.getExpiryDate() != null)
                .filter(patent -> !patent.getExpiryDate().isBefore(today))
                .filter(patent -> !patent.getExpiryDate().isBefore(startDate))
                .filter(patent -> !patent.getExpiryDate().isAfter(endDate))
                .forEach(patent -> byMonth
                        .computeIfAbsent(patent.getExpiryDate().getMonthValue(), ignored -> new ArrayList<>())
                        .add(patent));

        List<ExpiringPatentCalendarResponse.MonthBucket> months = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            List<ExpiringPatentCalendarResponse.PatentItem> patents = byMonth.getOrDefault(month, List.of()).stream()
                    .sorted(Comparator.comparing(Patent::getExpiryDate)
                            .thenComparing(Patent::getApplicationNumber)
                            .thenComparing(Patent::getId))
                    .map(ExpiringPatentCalendarResponse.PatentItem::from)
                    .toList();
            months.add(new ExpiringPatentCalendarResponse.MonthBucket(month, patents.size(), patents));
        }
        return new ExpiringPatentCalendarResponse(months);
    }

    private List<Patent> scopedPatents(User user) {
        List<Patent> patents;
        if (user.getRole() == UserRole.BUSINESS) {
            if (user.getDepartment() == null) {
                throw new ReviewException(ErrorCode.FORBIDDEN);
            }
            patents = patentRepository.findByCurrentDepartmentId(user.getDepartment().getId(), Pageable.unpaged()).getContent();
        } else {
            patents = patentRepository.findAll();
        }

        return patents.stream()
                .filter(patent -> patent.getApprovalStatus() == PatentApprovalStatus.APPROVED)
                .toList();
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

    private String normalizeGroupName(String value) {
        return value == null || value.isBlank() ? "미분류" : value;
    }

    private int normalizeListMonths(Integer months) {
        if (months == null) {
            return DEFAULT_LIST_PERIOD_MONTHS;
        }
        if (months <= 0) {
            throw new ReviewException(ErrorCode.INVALID_REQUEST);
        }
        return months;
    }
}
