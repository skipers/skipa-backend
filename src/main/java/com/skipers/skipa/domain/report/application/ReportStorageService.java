package com.skipers.skipa.domain.report.application;

public interface ReportStorageService {

    String generatePresignedUrl(String reportKey);
}
