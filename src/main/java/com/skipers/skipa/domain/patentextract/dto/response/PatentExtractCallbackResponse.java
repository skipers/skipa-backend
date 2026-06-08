package com.skipers.skipa.domain.patentextract.dto.response;

public record PatentExtractCallbackResponse(
        Long extractJobId,
        String status
) {

    public static PatentExtractCallbackResponse from(PatentExtractJobStatusResponse response) {
        return new PatentExtractCallbackResponse(
                response.extractJobId(),
                response.status()
        );
    }
}
