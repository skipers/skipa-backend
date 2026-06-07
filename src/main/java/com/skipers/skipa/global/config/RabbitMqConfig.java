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

@Configuration
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
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
