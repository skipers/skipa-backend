package com.skipers.skipa.domain.patentextract.dto.response;

import com.skipers.skipa.domain.patentextract.domain.PatentExtractJob;

import java.time.Instant;

public record PatentExtractJobStatusResponse(
        Long extractJobId,
        String objectKey,
        String status,
        String errorMessage,
        Instant uploadedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatentExtractJobStatusResponse from(PatentExtractJob job) {
        return new PatentExtractJobStatusResponse(
                job.getId(),
                job.getObjectKey(),
                job.getStatus().name(),
                job.getErrorMessage(),
                job.getUploadedAt(),
                job.getCompletedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
