package com.skipers.skipa.domain.preevaluation.dto.response;

import com.skipers.skipa.domain.chat.domain.ChatMessage;
import com.skipers.skipa.domain.chat.domain.ChatSourceCard;

import java.time.Instant;
import java.util.List;

public record PreEvaluationChatMessageResponse(
        Long id,
        Long preEvaluationId,
        String role,
        String content,
        List<ChatSourceCard> sourceCards,
        Instant createdAt
) {

    public static PreEvaluationChatMessageResponse from(ChatMessage message) {
        return new PreEvaluationChatMessageResponse(
                message.getId(),
                message.getTargetId(),
                message.getRole().name(),
                message.getContent(),
                message.getSourceCards(),
                message.getCreatedAt()
        );
    }
}
