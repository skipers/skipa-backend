package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.AnnuityHistory;

import java.time.Instant;
import java.time.LocalDate;

public record AnnuityResponse(
        Long id,
        Long patentId,
        Integer annuityYear,
        LocalDate dueDate,
        LocalDate paidDate,
        String status,
        Integer amount,
        Instant createdAt,
        Instant updatedAt
) {

    public static AnnuityResponse from(AnnuityHistory annuityHistory) {
        return new AnnuityResponse(
                annuityHistory.getId(),
                annuityHistory.getPatent().getId(),
                annuityHistory.getAnnuityYear(),
                annuityHistory.getDueDate(),
                annuityHistory.getPaidDate(),
                annuityHistory.getStatus().name(),
                annuityHistory.getAmount(),
                annuityHistory.getCreatedAt(),
                annuityHistory.getUpdatedAt()
        );
    }
}
