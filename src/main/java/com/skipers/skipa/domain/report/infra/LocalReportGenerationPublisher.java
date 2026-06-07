package com.skipers.skipa.domain.report.infra;

import com.skipers.skipa.domain.report.application.ReportGenerationPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalReportGenerationPublisher implements ReportGenerationPublisher {

    @Override
    public void publish(Long reportId, Long patentId) {
        // Local profile runs without RabbitMQ.
    }
}
