package com.skipers.skipa.domain.patentextract.dto.response;

import com.skipers.skipa.domain.patentextract.domain.PatentExtractJob;

import java.time.Instant;

public record PatentExtractResultResponse(
        Long extractJobId,
        String objectKey,
        String status,
        Object result,
        Instant uploadedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatentExtractResultResponse of(PatentExtractJob job, Object result) {
        return new PatentExtractResultResponse(
                job.getId(),
                job.getObjectKey(),
                job.getStatus().name(),
                result,
                job.getUploadedAt(),
                job.getCompletedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
