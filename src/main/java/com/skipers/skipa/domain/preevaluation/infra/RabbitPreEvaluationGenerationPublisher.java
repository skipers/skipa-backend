package com.skipers.skipa.domain.preevaluation.infra;

import com.skipers.skipa.domain.preevaluation.application.PreEvaluationGenerationPublisher;
import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
@RequiredArgsConstructor
public class RabbitPreEvaluationGenerationPublisher implements PreEvaluationGenerationPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.pre-evaluation.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.pre-evaluation.routing-key}")
    private String routingKey;

    @Override
    public void publish(PreEvaluation preEvaluation) {
        rabbitTemplate.convertAndSend(exchange, routingKey, PreEvaluationGenerationMessage.from(preEvaluation));
    }
}
