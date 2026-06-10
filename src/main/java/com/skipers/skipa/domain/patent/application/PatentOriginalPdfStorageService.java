package com.skipers.skipa.domain.patent.application;

import com.fasterxml.jackson.databind.JsonNode;

public interface PatentOriginalPdfStorageService {

    void copy(String sourceObjectKey, String targetObjectKey);

    void saveJson(String objectKey, JsonNode jsonNode);
}
