package com.skipers.skipa.domain.report.infra;

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
class RabbitReportGenerationPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publishSendsReportGenerateMessage() {
        RabbitReportGenerationPublisher publisher = new RabbitReportGenerationPublisher(rabbitTemplate);
        ReflectionTestUtils.setField(publisher, "exchange", "skipa.report.exchange");
        ReflectionTestUtils.setField(publisher, "routingKey", "report.generate");

        publisher.publish(8001L, 1001L);

        ArgumentCaptor<ReportGenerationMessage> messageCaptor = ArgumentCaptor.forClass(ReportGenerationMessage.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("skipa.report.exchange"),
                org.mockito.ArgumentMatchers.eq("report.generate"),
                messageCaptor.capture()
        );
        assertThat(messageCaptor.getValue().type()).isEqualTo("REPORT_GENERATE");
        assertThat(messageCaptor.getValue().reportId()).isEqualTo(8001L);
        assertThat(messageCaptor.getValue().patentId()).isEqualTo(1001L);
    }
}
