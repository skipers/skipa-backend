package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.Patent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PatentListResponse(
        Long id,
        String title,
        String applicationNumber,
        String registrationNumber,
        LocalDate applicationDate,
        LocalDate expiryDate,
        String applicant,
        String inventor,
        String latestLegalStatus,
        String techField,
        String businessField,
        String overview,
        List<String> keywords,
        Integer citationCount,
        String filingCountry,
        Long currentDepartmentId,
        String currentDepartmentName,
        String reviewStatus,
        String decision,
        Boolean checked,
        BigDecimal latestReportScore,
        boolean isOverdue,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatentListResponse from(Patent patent) {
        return of(patent, null, null, null, null, null, false);
    }

    public static PatentListResponse of(
            Patent patent,
            String latestLegalStatus,
            String reviewStatus,
            String decision,
            Boolean checked,
            BigDecimal latestReportScore,
            boolean isOverdue
    ) {
        Long currentDepartmentId = patent.getCurrentDepartment() == null ? null : patent.getCurrentDepartment().getId();
        String currentDepartmentName = patent.getCurrentDepartment() == null ? null : patent.getCurrentDepartment().getName();

        return new PatentListResponse(
                patent.getId(),
                patent.getTitle(),
                patent.getApplicationNumber(),
                patent.getRegistrationNumber(),
                patent.getApplicationDate(),
                patent.getExpiryDate(),
                patent.getApplicant(),
                patent.getInventor(),
                latestLegalStatus,
                patent.getTechField(),
                patent.getBusinessField(),
                patent.getOverview(),
                patent.getKeywords(),
                patent.getCitationCount(),
                patent.getFilingCountry(),
                currentDepartmentId,
                currentDepartmentName,
                reviewStatus,
                decision,
                checked,
                latestReportScore,
                isOverdue,
                patent.getCreatedAt(),
                patent.getUpdatedAt()
        );
    }
}
