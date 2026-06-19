package com.skipers.skipa.domain.preevaluation.infra;

import com.skipers.skipa.domain.preevaluation.application.PreEvaluationGenerationPublisher;
import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local | local-postgres")
public class LocalPreEvaluationGenerationPublisher implements PreEvaluationGenerationPublisher {

    @Override
    public void publish(PreEvaluation preEvaluation) {
        // Local profile runs without RabbitMQ.
    }
}
