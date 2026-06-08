package com.skipers.skipa.domain.patentextract.application;

public interface PatentExtractPublisher {

    void publish(Long extractJobId, String objectKey);
}
