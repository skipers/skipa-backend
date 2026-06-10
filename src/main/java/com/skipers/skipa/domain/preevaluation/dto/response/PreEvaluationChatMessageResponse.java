package com.skipers.skipa.domain.preevaluation.dto.response;

import com.skipers.skipa.domain.preevaluation.domain.PreEvaluationChatMessage;

import java.time.Instant;

public record PreEvaluationChatMessageResponse(
        Long id,
        Long preEvaluationId,
        String role,
        String content,
        Instant createdAt
) {

    public static PreEvaluationChatMessageResponse from(PreEvaluationChatMessage message) {
        return new PreEvaluationChatMessageResponse(
                message.getId(),
                message.getPreEvaluation().getId(),
                message.getRole().name(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
