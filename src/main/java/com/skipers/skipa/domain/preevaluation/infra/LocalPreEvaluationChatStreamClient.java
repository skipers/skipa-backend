package com.skipers.skipa.domain.preevaluation.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.chat.application.ChatStreamEvent;
import com.skipers.skipa.domain.preevaluation.application.PreEvaluationChatStreamClient;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationChatClientRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Component
@Profile("local | local-postgres")
public class LocalPreEvaluationChatStreamClient implements PreEvaluationChatStreamClient {

    private final ObjectMapper objectMapper;

    public LocalPreEvaluationChatStreamClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void stream(PreEvaluationChatClientRequest request, Consumer<ChatStreamEvent> eventConsumer) {
        emit(eventConsumer, "metadata", Map.of("query", request.question(), "patent_id", request.caseId(), "case_id", request.caseId(), "stream", true));
        emit(eventConsumer, "source_cards", Map.of("source_cards", java.util.List.of()));
        String answer = "로컬 환경에서는 AI 서버 사전 평가 스트리밍 응답을 생성하지 않습니다.";
        emit(eventConsumer, "delta", Map.of("text", answer));
        emit(eventConsumer, "done", Map.of(
                "query", request.question(),
                "patent_id", String.valueOf(request.caseId()),
                "case_id", String.valueOf(request.caseId()),
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
