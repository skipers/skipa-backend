package com.skipers.skipa.domain.report.infra;

import com.skipers.skipa.domain.report.application.ReportChatClient;
import com.skipers.skipa.domain.report.dto.request.ReportChatClientRequest;
import com.skipers.skipa.domain.report.dto.response.ReportChatClientResponse;
import com.skipers.skipa.domain.report.exception.ReportException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@Profile("!local")
public class RestReportChatClient implements ReportChatClient {

    private final RestClient restClient;
    private final String chatPath;

    public RestReportChatClient(
            @Value("${app.ai-server.base-url}") String baseUrl,
            @Value("${app.ai-server.report-chat-path}") String chatPath
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.chatPath = chatPath;
    }

    @Override
    public String send(ReportChatClientRequest request) {
        ReportChatClientResponse response = restClient.post()
                .uri(chatPath)
                .body(request)
                .retrieve()
                .body(ReportChatClientResponse.class);

        if (response == null || !StringUtils.hasText(response.message())) {
            throw new ReportException(ErrorCode.AI_SERVER_ERROR);
        }

        return response.message();
    }
}
