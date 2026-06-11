package com.skipers.skipa.domain.preevaluation.dto.response;

import com.skipers.skipa.domain.chat.domain.ChatMessage;

import java.time.Instant;

public record PreEvaluationChatMessageResponse(
        Long id,
        Long preEvaluationId,
        String role,
        String content,
        Instant createdAt
) {

    public static PreEvaluationChatMessageResponse from(ChatMessage message) {
        return new PreEvaluationChatMessageResponse(
                message.getId(),
                message.getTargetId(),
                message.getRole().name(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
