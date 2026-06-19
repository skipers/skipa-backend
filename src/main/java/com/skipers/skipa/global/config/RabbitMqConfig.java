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
@Profile("!local & !local-postgres")
public class RabbitMqConfig {

    @Bean
    public TopicExchange rabbitExchange(@Value("${app.rabbitmq.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue reportGenerateQueue(@Value("${app.rabbitmq.report.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding reportGenerateBinding(
            Queue reportGenerateQueue,
            TopicExchange rabbitExchange,
            @Value("${app.rabbitmq.report.routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(reportGenerateQueue).to(rabbitExchange).with(routingKey);
    }

    @Bean
    public Queue patentExtractQueue(@Value("${app.rabbitmq.patent-extract.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding patentExtractBinding(
            Queue patentExtractQueue,
            TopicExchange rabbitExchange,
            @Value("${app.rabbitmq.patent-extract.routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(patentExtractQueue).to(rabbitExchange).with(routingKey);
    }

    @Bean
    public Queue preEvaluationGenerateQueue(@Value("${app.rabbitmq.pre-evaluation.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding preEvaluationGenerateBinding(
            Queue preEvaluationGenerateQueue,
            TopicExchange rabbitExchange,
            @Value("${app.rabbitmq.pre-evaluation.routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(preEvaluationGenerateQueue).to(rabbitExchange).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
