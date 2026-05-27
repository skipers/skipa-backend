package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.Patent;

import java.time.Instant;

public record PatentListResponse(
        Long id,
        String title,
        String applicationNumber,
        String applicant,
        String inventor,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatentListResponse from(Patent patent) {
        return new PatentListResponse(
                patent.getId(),
                patent.getTitle(),
                patent.getApplicationNumber(),
                patent.getApplicant(),
                patent.getInventor(),
                patent.getCreatedAt(),
                patent.getUpdatedAt()
        );
    }
}
