package com.skipers.skipa.domain.report.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.skipers.skipa.domain.report.domain.Report;

import java.util.List;

public record ReportChatClientRequest(
        @JsonIgnore
        Long patentId,
        @JsonProperty("user_id")
        String userId,
        String question,
        @JsonProperty("chat_history")
        List<History> chatHistory
) {

    public static ReportChatClientRequest of(
            Report report,
            Long userId,
            String question,
            List<History> chatHistory
    ) {
        return new ReportChatClientRequest(
                report.getPatent().getId(),
                String.valueOf(userId),
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
