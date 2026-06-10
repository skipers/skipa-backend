package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.Patent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PatentDetailResponse(
        Long id,
        String title,
        String applicationNumber,
        String registrationNumber,
        String publicationNumber,
        String announcementNumber,
        LocalDate applicationDate,
        LocalDate registrationDate,
        LocalDate publicationDate,
        LocalDate announcementDate,
        List<String> ipcCodes,
        List<String> cpcCodes,
        String applicant,
        String inventor,
        LocalDate expiryDate,
        Integer citationCount,
        Integer examinationClaimCount,
        String originalPdfKey,
        String parsedJsonKey,
        String approvalStatus,
        String managementNumber,
        String businessField,
        String techField,
        List<String> relatedProducts,
        String filingCountry,
        Boolean isJointApplication,
        String jointApplicant,
        String initialDepartment,
        Long currentDepartmentId,
        String currentDepartmentName,
        String latestLegalStatus,
        BigDecimal latestReportScore,
        List<String> keywords,
        String summary,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatentDetailResponse from(Patent patent) {
        return of(patent, null, null);
    }

    public static PatentDetailResponse of(Patent patent, String latestLegalStatus, BigDecimal latestReportScore) {
        Long currentDepartmentId = patent.getCurrentDepartment() == null ? null : patent.getCurrentDepartment().getId();
        String currentDepartmentName = patent.getCurrentDepartment() == null ? null : patent.getCurrentDepartment().getName();

        return new PatentDetailResponse(
                patent.getId(),
                patent.getTitle(),
                patent.getApplicationNumber(),
                patent.getRegistrationNumber(),
                patent.getPublicationNumber(),
                patent.getAnnouncementNumber(),
                patent.getApplicationDate(),
                patent.getRegistrationDate(),
                patent.getPublicationDate(),
                patent.getAnnouncementDate(),
                patent.getIpcCodes(),
                patent.getCpcCodes(),
                patent.getApplicant(),
                patent.getInventor(),
                patent.getExpiryDate(),
                patent.getCitationCount(),
                patent.getExaminationClaimCount(),
                patent.getOriginalPdfKey(),
                patent.getParsedJsonKey(),
                patent.getApprovalStatus().name(),
                patent.getManagementNumber(),
                patent.getBusinessField(),
                patent.getTechField(),
                patent.getRelatedProducts(),
                patent.getFilingCountry(),
                patent.getIsJointApplication(),
                patent.getJointApplicant(),
                patent.getInitialDepartment(),
                currentDepartmentId,
                currentDepartmentName,
                latestLegalStatus,
                latestReportScore,
                patent.getKeywords(),
                patent.getSummary(),
                patent.getCreatedAt(),
                patent.getUpdatedAt()
        );
    }
}
