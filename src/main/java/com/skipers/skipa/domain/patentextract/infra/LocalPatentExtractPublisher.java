package com.skipers.skipa.domain.patentextract.infra;

import com.skipers.skipa.domain.patentextract.application.PatentExtractPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalPatentExtractPublisher implements PatentExtractPublisher {

    @Override
    public void publish(Long extractJobId, String objectKey) {
        // Local profile runs without RabbitMQ.
    }
}
