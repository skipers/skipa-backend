package com.skipers.skipa.domain.preevaluation.dto.response;

import com.skipers.skipa.domain.preevaluation.domain.PreEvaluationChatMessage;

public record PreEvaluationChatSendResponse(
        PreEvaluationChatMessageResponse userMessage,
        PreEvaluationChatMessageResponse assistantMessage
) {

    public static PreEvaluationChatSendResponse of(
            PreEvaluationChatMessage userMessage,
            PreEvaluationChatMessage assistantMessage
    ) {
        return new PreEvaluationChatSendResponse(
                PreEvaluationChatMessageResponse.from(userMessage),
                PreEvaluationChatMessageResponse.from(assistantMessage)
        );
    }
}
