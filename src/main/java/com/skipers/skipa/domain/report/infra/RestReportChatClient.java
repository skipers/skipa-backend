package com.skipers.skipa.domain.report.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.chat.dto.ChatClientResult;
import com.skipers.skipa.domain.report.application.ReportChatClient;
import com.skipers.skipa.domain.report.dto.request.ReportChatClientRequest;
import com.skipers.skipa.domain.report.dto.response.ReportChatClientResponse;
import com.skipers.skipa.domain.report.exception.ReportException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
@Profile("!local")
public class RestReportChatClient implements ReportChatClient {

    private final RestClient restClient;
    private final String chatPath;
    private final ObjectMapper objectMapper;

    public RestReportChatClient(
            @Value("${app.ai-server.base-url}") String baseUrl,
            @Value("${app.ai-server.report-chat-path}") String chatPath,
            ObjectMapper objectMapper
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(3000));
        requestFactory.setReadTimeout(Duration.ofMillis(120000));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.chatPath = chatPath;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatClientResult send(ReportChatClientRequest request) {
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new ReportException(ErrorCode.AI_SERVER_ERROR, e);
        }

        ReportChatClientResponse response = restClient.post()
                .uri(chatPath, request.patentId())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(ReportChatClientResponse.class);

        if (response == null || !StringUtils.hasText(response.answer())) {
            throw new ReportException(ErrorCode.AI_SERVER_ERROR);
        }

        return response.toResult();
    }
}
