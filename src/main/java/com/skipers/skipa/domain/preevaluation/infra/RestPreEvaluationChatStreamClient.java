package com.skipers.skipa.domain.preevaluation.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.chat.application.ChatStreamEvent;
import com.skipers.skipa.domain.chat.infra.AiChatStreamClientSupport;
import com.skipers.skipa.domain.preevaluation.application.PreEvaluationChatStreamClient;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationChatClientRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@Profile("!local")
public class RestPreEvaluationChatStreamClient extends AiChatStreamClientSupport<PreEvaluationChatClientRequest>
        implements PreEvaluationChatStreamClient {

    private final String streamPath;

    public RestPreEvaluationChatStreamClient(
            @Value("${app.ai-server.base-url}") String baseUrl,
            @Value("${app.ai-server.pre-evaluation-chat-stream-path}") String streamPath,
            @Value("${app.ai-server.chat-stream-connect-timeout-ms:3000}") long connectTimeoutMs,
            ObjectMapper objectMapper
    ) {
        super(baseUrl, connectTimeoutMs, objectMapper);
        this.streamPath = streamPath;
    }

    @Override
    public void stream(PreEvaluationChatClientRequest request, Consumer<ChatStreamEvent> eventConsumer) {
        stream(request, streamPath.replace("{case_id}", String.valueOf(request.caseId())), eventConsumer);
    }
}
