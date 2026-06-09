package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.PatentAnnuity;

import java.time.Instant;
import java.time.LocalDate;

public record PatentAnnuityResponse(
        Long id,
        Long patentId,
        Integer startYear,
        Integer endYear,
        LocalDate dueDate,
        LocalDate paidDate,
        String status,
        Integer amount,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatentAnnuityResponse from(PatentAnnuity patentAnnuity) {
        return new PatentAnnuityResponse(
                patentAnnuity.getId(),
                patentAnnuity.getPatent().getId(),
                patentAnnuity.getStartYear(),
                patentAnnuity.getEndYear(),
                patentAnnuity.getDueDate(),
                patentAnnuity.getPaidDate(),
                patentAnnuity.getStatus().name(),
                patentAnnuity.getAmount(),
                patentAnnuity.getCreatedAt(),
                patentAnnuity.getUpdatedAt()
        );
    }
}
