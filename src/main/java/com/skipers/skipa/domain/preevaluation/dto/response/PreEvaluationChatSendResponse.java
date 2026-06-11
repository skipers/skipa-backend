package com.skipers.skipa.domain.preevaluation.dto.response;

import com.skipers.skipa.domain.chat.domain.ChatMessage;

public record PreEvaluationChatSendResponse(
        PreEvaluationChatMessageResponse userMessage,
        PreEvaluationChatMessageResponse assistantMessage
) {

    public static PreEvaluationChatSendResponse of(
            ChatMessage userMessage,
            ChatMessage assistantMessage
    ) {
        return new PreEvaluationChatSendResponse(
                PreEvaluationChatMessageResponse.from(userMessage),
                PreEvaluationChatMessageResponse.from(assistantMessage)
        );
    }
}
