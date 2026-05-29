package com.skipers.skipa.domain.patent_legal_status.dto.response;

import com.skipers.skipa.domain.patent_legal_status.domain.PatentLegalStatus;

import java.time.Instant;
import java.time.LocalDate;

public record PatentLegalStatusResponse(
        Long id,
        Long patentId,
        String status,
        LocalDate changedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatentLegalStatusResponse from(PatentLegalStatus patentLegalStatus) {
        return new PatentLegalStatusResponse(
                patentLegalStatus.getId(),
                patentLegalStatus.getPatent().getId(),
                patentLegalStatus.getStatus(),
                patentLegalStatus.getChangedAt(),
                patentLegalStatus.getCreatedAt(),
                patentLegalStatus.getUpdatedAt()
        );
    }
}

