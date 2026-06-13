package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.Patent;

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
        String approvalStatus,
        String rejectionReason,
        Long currentDepartmentId,
        String currentDepartmentName,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatentListResponse from(Patent patent) {
        return of(patent, null);
    }

    public static PatentListResponse of(
            Patent patent,
            String latestLegalStatus
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
                patent.getApprovalStatus().name(),
                patent.getRejectionReason(),
                currentDepartmentId,
                currentDepartmentName,
                patent.getCreatedAt(),
                patent.getUpdatedAt()
        );
    }
}
