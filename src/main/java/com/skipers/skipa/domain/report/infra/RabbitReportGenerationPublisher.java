package com.skipers.skipa.domain.report.infra;

import com.skipers.skipa.domain.report.application.ReportGenerationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
@RequiredArgsConstructor
public class RabbitReportGenerationPublisher implements ReportGenerationPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.report.routing-key}")
    private String routingKey;

    @Override
    public void publish(Long reportId, Long patentId) {
        rabbitTemplate.convertAndSend(exchange, routingKey, ReportGenerationMessage.of(reportId, patentId));
    }
}
