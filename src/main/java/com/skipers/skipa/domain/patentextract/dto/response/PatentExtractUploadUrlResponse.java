package com.skipers.skipa.domain.patentextract.dto.response;

import com.skipers.skipa.domain.patentextract.domain.PatentExtractJob;

import java.time.Instant;

public record PatentExtractUploadUrlResponse(
        Long extractJobId,
        String objectKey,
        String uploadUrl,
        Integer expiresInSeconds,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatentExtractUploadUrlResponse of(
            PatentExtractJob job,
            String uploadUrl,
            Integer expiresInSeconds
    ) {
        return new PatentExtractUploadUrlResponse(
                job.getId(),
                job.getObjectKey(),
                uploadUrl,
                expiresInSeconds,
                job.getStatus().name(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
