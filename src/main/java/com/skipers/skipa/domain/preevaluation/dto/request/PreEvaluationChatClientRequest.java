package com.skipers.skipa.domain.preevaluation.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;

import java.util.List;

public record PreEvaluationChatClientRequest(
        @JsonIgnore
        Long caseId,
        @JsonProperty("user_id")
        String userId,
        String question,
        @JsonProperty("chat_history")
        List<History> chatHistory
) {

    public static PreEvaluationChatClientRequest of(
            PreEvaluation preEvaluation,
            String question,
            List<History> chatHistory
    ) {
        return new PreEvaluationChatClientRequest(
                preEvaluation.getId(),
                String.valueOf(preEvaluation.getUser().getId()),
                question,
                chatHistory
        );
    }

    public record History(
            String question,
            String answer
    ) {
    }
}
