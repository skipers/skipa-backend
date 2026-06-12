package com.skipers.skipa.domain.preevaluation.application;

import com.skipers.skipa.domain.chat.dao.ChatMessageRepository;
import com.skipers.skipa.domain.chat.domain.ChatMessage;
import com.skipers.skipa.domain.chat.domain.ChatRole;
import com.skipers.skipa.domain.chat.domain.ChatTargetType;
import com.skipers.skipa.domain.chat.dto.ChatClientResult;
import com.skipers.skipa.domain.preevaluation.dao.PreEvaluationRepository;
import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreEvaluationChatService {

    private final PreEvaluationRepository preEvaluationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PreEvaluationChatClient chatClient;

    public List<PreEvaluationChatMessageResponse> getMessages(User user, Long preEvaluationId) {
        PreEvaluation preEvaluation = getOwnedPreEvaluation(user, preEvaluationId);

        return chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                        ChatTargetType.PRE_EVALUATION,
                        preEvaluation.getId()
                )
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
        List<ChatMessage> previousMessages = chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                ChatTargetType.PRE_EVALUATION,
                preEvaluation.getId()
        );

        ChatMessage userMessage = chatMessageRepository.save(ChatMessage.builder()
                .targetType(ChatTargetType.PRE_EVALUATION)
                .targetId(preEvaluation.getId())
                .user(user)
                .role(ChatRole.USER)
                .content(request.message())
                .build());

        ChatClientResult aiResult = sendToAiServer(preEvaluation, request.message(), toRecentHistory(previousMessages));
        ChatMessage assistantMessage = chatMessageRepository.save(ChatMessage.builder()
                .targetType(ChatTargetType.PRE_EVALUATION)
                .targetId(preEvaluation.getId())
                .user(user)
                .role(ChatRole.ASSISTANT)
                .content(aiResult.answer())
                .sourceCards(aiResult.sourceCards())
                .build());

        return PreEvaluationChatSendResponse.of(userMessage, assistantMessage);
    }

    @Transactional
    public void clearMessages(User user, Long preEvaluationId) {
        PreEvaluation preEvaluation = getOwnedPreEvaluation(user, preEvaluationId);
        chatMessageRepository.deleteAllByTargetTypeAndTargetId(ChatTargetType.PRE_EVALUATION, preEvaluation.getId());
    }

    private ChatClientResult sendToAiServer(
            PreEvaluation preEvaluation,
            String message,
            List<PreEvaluationChatClientRequest.History> history
    ) {
        try {
            return chatClient.send(PreEvaluationChatClientRequest.of(preEvaluation, message, history));
        } catch (RuntimeException e) {
            throw new PreEvaluationException(ErrorCode.AI_SERVER_ERROR, e);
        }
    }

    private List<PreEvaluationChatClientRequest.History> toRecentHistory(List<ChatMessage> messages) {
        List<PreEvaluationChatClientRequest.History> histories = new java.util.ArrayList<>();
        String pendingQuestion = null;

        for (ChatMessage message : messages) {
            if (message.getRole() == ChatRole.USER) {
                pendingQuestion = message.getContent();
            } else if (message.getRole() == ChatRole.ASSISTANT && pendingQuestion != null) {
                histories.add(new PreEvaluationChatClientRequest.History(pendingQuestion, message.getContent()));
                pendingQuestion = null;
            }
        }

        int fromIndex = Math.max(0, histories.size() - 5);
        return histories.subList(fromIndex, histories.size());
    }

    private PreEvaluation getOwnedPreEvaluation(User user, Long preEvaluationId) {
        return preEvaluationRepository.findByIdAndUserId(preEvaluationId, user.getId())
                .orElseThrow(() -> new PreEvaluationException(ErrorCode.PRE_EVALUATION_NOT_FOUND));
    }
}
