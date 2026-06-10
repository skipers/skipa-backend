package com.skipers.skipa.domain.patentextract.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitPatentExtractPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publishSendsPatentExtractMessage() {
        RabbitPatentExtractPublisher publisher = new RabbitPatentExtractPublisher(rabbitTemplate);
        ReflectionTestUtils.setField(publisher, "exchange", "skipa.patent-extract.exchange");
        ReflectionTestUtils.setField(publisher, "routingKey", "patent.extract");

        publisher.publish(1L, "tmp/patent-extract-jobs/1/original.pdf");

        ArgumentCaptor<PatentExtractMessage> messageCaptor = ArgumentCaptor.forClass(PatentExtractMessage.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("skipa.patent-extract.exchange"),
                org.mockito.ArgumentMatchers.eq("patent.extract"),
                messageCaptor.capture()
        );
        assertThat(messageCaptor.getValue().type()).isEqualTo("PATENT_EXTRACT");
        assertThat(messageCaptor.getValue().extractJobId()).isEqualTo(1L);
        assertThat(messageCaptor.getValue().objectKey()).isEqualTo("tmp/patent-extract-jobs/1/original.pdf");
    }
}
