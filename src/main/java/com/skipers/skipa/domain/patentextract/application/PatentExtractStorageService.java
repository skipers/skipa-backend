package com.skipers.skipa.domain.patentextract.application;

public interface PatentExtractStorageService {

    String generateUploadPresignedUrl(String objectKey);

    boolean exists(String objectKey);
}
