package com.skipers.skipa.domain.patentextract.infra;

public record PatentExtractMessage(
        String type,
        Long extractJobId,
        String objectKey
) {

    private static final String PATENT_EXTRACT = "PATENT_EXTRACT";

    public static PatentExtractMessage of(Long extractJobId, String objectKey) {
        return new PatentExtractMessage(PATENT_EXTRACT, extractJobId, objectKey);
    }
}
