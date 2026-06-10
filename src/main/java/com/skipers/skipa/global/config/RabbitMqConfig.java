package com.skipers.skipa.global.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!local")
public class RabbitMqConfig {

    @Bean
    public TopicExchange reportExchange(@Value("${app.rabbitmq.report.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue reportGenerateQueue(@Value("${app.rabbitmq.report.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding reportGenerateBinding(
            Queue reportGenerateQueue,
            TopicExchange reportExchange,
            @Value("${app.rabbitmq.report.routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(reportGenerateQueue).to(reportExchange).with(routingKey);
    }

    @Bean
    public TopicExchange patentExtractExchange(@Value("${app.rabbitmq.patent-extract.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue patentExtractQueue(@Value("${app.rabbitmq.patent-extract.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding patentExtractBinding(
            Queue patentExtractQueue,
            TopicExchange patentExtractExchange,
            @Value("${app.rabbitmq.patent-extract.routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(patentExtractQueue).to(patentExtractExchange).with(routingKey);
    }

    @Bean
    public TopicExchange preEvaluationExchange(@Value("${app.rabbitmq.pre-evaluation.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue preEvaluationGenerateQueue(@Value("${app.rabbitmq.pre-evaluation.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding preEvaluationGenerateBinding(
            Queue preEvaluationGenerateQueue,
            TopicExchange preEvaluationExchange,
            @Value("${app.rabbitmq.pre-evaluation.routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(preEvaluationGenerateQueue).to(preEvaluationExchange).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
