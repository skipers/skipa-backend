package com.skipers.skipa.domain.patentextract.dto.response;

import java.time.Instant;

public record PatentExtractCallbackResponse(
        Long extractJobId,
        String objectKey,
        String status,
        String errorMessage,
        Instant uploadedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatentExtractCallbackResponse from(PatentExtractJobStatusResponse response) {
        return new PatentExtractCallbackResponse(
                response.extractJobId(),
                response.objectKey(),
                response.status(),
                response.errorMessage(),
                response.uploadedAt(),
                response.completedAt(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}
