package com.skipers.skipa.domain.report.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skipers.skipa.domain.chat.domain.ChatSourceCard;
import com.skipers.skipa.domain.chat.dto.ChatClientResult;

import java.util.List;
import java.util.Map;

public record ReportChatClientResponse(
        String query,
        @JsonProperty("patent_id")
        String patentId,
        String answer,
        @JsonProperty("source_cards")
        List<ChatSourceCard> sourceCards,
        Map<String, Object> metrics
) {

    public ChatClientResult toResult() {
        return new ChatClientResult(answer, sourceCards);
    }
}
