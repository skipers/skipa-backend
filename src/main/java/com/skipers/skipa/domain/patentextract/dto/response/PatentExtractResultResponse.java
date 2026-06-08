package com.skipers.skipa.domain.patentextract.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.skipers.skipa.domain.patentextract.domain.PatentExtractJob;

import java.time.Instant;

public record PatentExtractResultResponse(
        Long extractJobId,
        String objectKey,
        String status,
        JsonNode result,
        Instant uploadedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatentExtractResultResponse from(PatentExtractJob job) {
        return new PatentExtractResultResponse(
                job.getId(),
                job.getObjectKey(),
                job.getStatus().name(),
                job.getResultJson(),
                job.getUploadedAt(),
                job.getCompletedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
