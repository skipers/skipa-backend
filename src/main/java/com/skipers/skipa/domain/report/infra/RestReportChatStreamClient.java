package com.skipers.skipa.domain.report.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.chat.application.ChatStreamEvent;
import com.skipers.skipa.domain.chat.infra.AiChatStreamClientSupport;
import com.skipers.skipa.domain.report.application.ReportChatStreamClient;
import com.skipers.skipa.domain.report.dto.request.ReportChatClientRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@Profile("!local & !local-postgres")
public class RestReportChatStreamClient extends AiChatStreamClientSupport<ReportChatClientRequest>
        implements ReportChatStreamClient {

    private final String streamPath;

    public RestReportChatStreamClient(
            @Value("${app.ai-server.base-url}") String baseUrl,
            @Value("${app.ai-server.report-chat-stream-path}") String streamPath,
            @Value("${app.ai-server.chat-stream-connect-timeout-ms:3000}") long connectTimeoutMs,
            ObjectMapper objectMapper
    ) {
        super(baseUrl, connectTimeoutMs, objectMapper);
        this.streamPath = streamPath;
    }

    @Override
    public void stream(ReportChatClientRequest request, Consumer<ChatStreamEvent> eventConsumer) {
        stream(request, streamPath.replace("{patent_id}", String.valueOf(request.patentId())), eventConsumer);
    }
}
