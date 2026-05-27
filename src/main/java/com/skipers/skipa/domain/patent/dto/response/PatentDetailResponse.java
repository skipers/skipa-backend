package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.Patent;

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
        String ipcCode,
        String cpcCode,
        String applicant,
        String inventor,
        LocalDate expiryDate,
        Integer citationCount,
        String originalPdfKey,
        String managementNumber,
        String businessField,
        String techField,
        List<String> relatedProducts,
        String filingCountry,
        Boolean isJointApplication,
        String jointApplicant,
        String initialDepartment,
        List<String> keywords,
        String overview,
        String coreContent,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatentDetailResponse from(
            Patent patent,
            List<String> relatedProducts,
            List<String> keywords
    ) {
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
                patent.getIpcCode(),
                patent.getCpcCode(),
                patent.getApplicant(),
                patent.getInventor(),
                patent.getExpiryDate(),
                patent.getCitationCount(),
                patent.getOriginalPdfKey(),
                patent.getManagementNumber(),
                patent.getBusinessField(),
                patent.getTechField(),
                relatedProducts,
                patent.getFilingCountry(),
                patent.getIsJointApplication(),
                patent.getJointApplicant(),
                patent.getInitialDepartment(),
                keywords,
                patent.getOverview(),
                patent.getCoreContent(),
                patent.getCreatedAt(),
                patent.getUpdatedAt()
        );
    }
}
