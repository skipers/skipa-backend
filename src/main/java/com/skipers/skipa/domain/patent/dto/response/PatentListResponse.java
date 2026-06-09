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
        List<String> ipcCodes,
        List<String> cpcCodes,
        String applicant,
        String inventor,
        String latestLegalStatus,
        String techField,
        String businessField,
        List<String> keywords,
        String summary,
        Integer citationCount,
        Integer examinationClaimCount,
        String filingCountry,
        Long currentDepartmentId,
        String currentDepartmentName,
        String reviewStatus,
        String opinion,
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
            String opinion,
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
                patent.getIpcCodes(),
                patent.getCpcCodes(),
                patent.getApplicant(),
                patent.getInventor(),
                latestLegalStatus,
                patent.getTechField(),
                patent.getBusinessField(),
                patent.getKeywords(),
                patent.getSummary(),
                patent.getCitationCount(),
                patent.getExaminationClaimCount(),
                patent.getFilingCountry(),
                currentDepartmentId,
                currentDepartmentName,
                reviewStatus,
                opinion,
                checked,
                latestReportScore,
                isOverdue,
                patent.getCreatedAt(),
                patent.getUpdatedAt()
        );
    }
}
