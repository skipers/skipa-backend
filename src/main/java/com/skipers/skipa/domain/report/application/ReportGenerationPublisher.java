package com.skipers.skipa.domain.report.application;

public interface ReportGenerationPublisher {

    void publish(Long reportId, Long patentId);
}
