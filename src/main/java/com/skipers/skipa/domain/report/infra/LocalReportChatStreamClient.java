package com.skipers.skipa.domain.report.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.chat.application.ChatStreamEvent;
import com.skipers.skipa.domain.report.application.ReportChatStreamClient;
import com.skipers.skipa.domain.report.dto.request.ReportChatClientRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Profile("local | local-postgres")
public class LocalReportChatStreamClient implements ReportChatStreamClient {

    private final ObjectMapper objectMapper;

    public LocalReportChatStreamClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void stream(ReportChatClientRequest request, Consumer<ChatStreamEvent> eventConsumer) {
        emit(eventConsumer, "metadata", Map.of("query", request.question(), "patent_id", request.patentId(), "stream", true));
        emit(eventConsumer, "source_cards", Map.of("source_cards", java.util.List.of()));
        String answer = "로컬 환경에서는 AI 서버 평가 보고서 스트리밍 응답을 생성하지 않습니다.";
        emit(eventConsumer, "delta", Map.of("text", answer));
        emit(eventConsumer, "done", Map.of(
                "query", request.question(),
                "patent_id", String.valueOf(request.patentId()),
                "answer", answer,
                "source_cards", java.util.List.of(),
                "metrics", Map.of("stream", true),
                "stream", true
        ));
    }

    private void emit(Consumer<ChatStreamEvent> eventConsumer, String event, Map<String, Object> data) {
        try {
            String raw = "event: " + event + "\n" + "data: " + objectMapper.writeValueAsString(data) + "\n\n";
            eventConsumer.accept(new ChatStreamEvent(event, objectMapper.valueToTree(data), raw));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create local stream event", e);
        }
    }
}
