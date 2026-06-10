package com.skipers.skipa.domain.preevaluation.application;

import com.skipers.skipa.domain.preevaluation.dao.PreEvaluationChatMessageRepository;
import com.skipers.skipa.domain.preevaluation.dao.PreEvaluationRepository;
import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;
import com.skipers.skipa.domain.preevaluation.domain.PreEvaluationChatMessage;
import com.skipers.skipa.domain.preevaluation.domain.PreEvaluationChatRole;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationChatClientRequest;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationChatMessageRequest;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationChatMessageResponse;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationChatSendResponse;
import com.skipers.skipa.domain.preevaluation.exception.PreEvaluationException;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreEvaluationChatService {

    private final PreEvaluationRepository preEvaluationRepository;
    private final PreEvaluationChatMessageRepository chatMessageRepository;
    private final PreEvaluationChatClient chatClient;

    public List<PreEvaluationChatMessageResponse> getMessages(User user, Long preEvaluationId) {
        PreEvaluation preEvaluation = getOwnedPreEvaluation(user, preEvaluationId);

        return chatMessageRepository.findByPreEvaluationIdOrderByCreatedAtAsc(preEvaluation.getId())
                .stream()
                .map(PreEvaluationChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public PreEvaluationChatSendResponse sendMessage(
            User user,
            Long preEvaluationId,
            PreEvaluationChatMessageRequest request
    ) {
        PreEvaluation preEvaluation = getOwnedPreEvaluation(user, preEvaluationId);
        List<PreEvaluationChatMessage> previousMessages =
                chatMessageRepository.findByPreEvaluationIdOrderByCreatedAtAsc(preEvaluation.getId());

        PreEvaluationChatMessage userMessage = chatMessageRepository.save(PreEvaluationChatMessage.builder()
                .preEvaluation(preEvaluation)
                .role(PreEvaluationChatRole.USER)
                .content(request.message())
                .build());

        List<PreEvaluationChatClientRequest.Message> history = new ArrayList<>(previousMessages.stream()
                .map(PreEvaluationChatClientRequest.Message::from)
                .toList());
        history.add(PreEvaluationChatClientRequest.Message.from(userMessage));

        String assistantContent = sendToAiServer(preEvaluation, request.message(), history);
        PreEvaluationChatMessage assistantMessage = chatMessageRepository.save(PreEvaluationChatMessage.builder()
                .preEvaluation(preEvaluation)
                .role(PreEvaluationChatRole.ASSISTANT)
                .content(assistantContent)
                .build());

        return PreEvaluationChatSendResponse.of(userMessage, assistantMessage);
    }

    @Transactional
    public void clearMessages(User user, Long preEvaluationId) {
        PreEvaluation preEvaluation = getOwnedPreEvaluation(user, preEvaluationId);
        chatMessageRepository.deleteAllByPreEvaluationId(preEvaluation.getId());
    }

    private String sendToAiServer(
            PreEvaluation preEvaluation,
            String message,
            List<PreEvaluationChatClientRequest.Message> history
    ) {
        try {
            return chatClient.send(PreEvaluationChatClientRequest.of(preEvaluation, message, history));
        } catch (RuntimeException e) {
            throw new PreEvaluationException(ErrorCode.AI_SERVER_ERROR, e);
        }
    }

    private PreEvaluation getOwnedPreEvaluation(User user, Long preEvaluationId) {
        return preEvaluationRepository.findByIdAndUserId(preEvaluationId, user.getId())
                .orElseThrow(() -> new PreEvaluationException(ErrorCode.PRE_EVALUATION_NOT_FOUND));
    }
}
