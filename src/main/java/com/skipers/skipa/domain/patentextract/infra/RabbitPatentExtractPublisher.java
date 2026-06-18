package com.skipers.skipa.domain.patentextract.infra;

import com.skipers.skipa.domain.patentextract.application.PatentExtractPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local & !local-postgres")
@RequiredArgsConstructor
public class RabbitPatentExtractPublisher implements PatentExtractPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.patent-extract.routing-key}")
    private String routingKey;

    @Override
    public void publish(Long extractJobId, String objectKey) {
        rabbitTemplate.convertAndSend(exchange, routingKey, PatentExtractMessage.of(extractJobId, objectKey));
    }
}
