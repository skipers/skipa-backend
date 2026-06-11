package com.skipers.skipa.domain.preevaluation.dto.request;

import com.skipers.skipa.domain.chat.domain.ChatMessage;
import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;

import java.util.List;

public record PreEvaluationChatClientRequest(
        Long preEvaluationId,
        Long userId,
        String title,
        String technicalDescription,
        List<String> claims,
        String relatedBusiness,
        String targetCountries,
        String message,
        List<Message> history
) {

    public static PreEvaluationChatClientRequest of(
            PreEvaluation preEvaluation,
            String message,
            List<Message> history
    ) {
        return new PreEvaluationChatClientRequest(
                preEvaluation.getId(),
                preEvaluation.getUser().getId(),
                preEvaluation.getTitle(),
                preEvaluation.getTechnicalDescription(),
                preEvaluation.getClaims(),
                preEvaluation.getRelatedBusiness(),
                preEvaluation.getTargetCountries(),
                message,
                history
        );
    }

    public record Message(
            String role,
            String content
    ) {

        public static Message from(ChatMessage message) {
            return new Message(message.getRole().name(), message.getContent());
        }
    }
}
