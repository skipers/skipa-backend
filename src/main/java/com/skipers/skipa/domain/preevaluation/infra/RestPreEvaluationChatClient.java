package com.skipers.skipa.domain.preevaluation.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.chat.dto.ChatClientResult;
import com.skipers.skipa.domain.preevaluation.application.PreEvaluationChatClient;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationChatClientRequest;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationChatClientResponse;
import com.skipers.skipa.domain.preevaluation.exception.PreEvaluationException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@Profile("!local")
public class RestPreEvaluationChatClient implements PreEvaluationChatClient {

    private final RestClient restClient;
    private final String chatPath;
    private final ObjectMapper objectMapper;

    public RestPreEvaluationChatClient(
            @Value("${app.ai-server.base-url}") String baseUrl,
            @Value("${app.ai-server.pre-evaluation-chat-path}") String chatPath,
            ObjectMapper objectMapper
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.chatPath = chatPath;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatClientResult send(PreEvaluationChatClientRequest request) {
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new PreEvaluationException(ErrorCode.AI_SERVER_ERROR, e);
        }

        PreEvaluationChatClientResponse response = restClient.post()
                .uri(chatPath, request.caseId())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(PreEvaluationChatClientResponse.class);

        if (response == null || !StringUtils.hasText(response.answer())) {
            throw new PreEvaluationException(ErrorCode.AI_SERVER_ERROR);
        }

        return response.toResult();
    }
}
