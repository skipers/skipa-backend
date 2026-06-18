package com.skipers.skipa.domain.patent.application;

public interface PatentOriginalPdfStorageService {

    void copy(String sourceObjectKey, String targetObjectKey);

    String generatePresignedUrl(String originalPdfKey);
}
