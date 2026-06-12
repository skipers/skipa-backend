package com.skipers.skipa.domain.report.dto.response;

import com.skipers.skipa.domain.chat.domain.ChatMessage;
import com.skipers.skipa.domain.chat.domain.ChatSourceCard;

import java.time.Instant;
import java.util.List;

public record ReportChatMessageResponse(
        Long id,
        Long reportId,
        String role,
        String content,
        List<ChatSourceCard> sourceCards,
        Instant createdAt
) {

    public static ReportChatMessageResponse from(ChatMessage message) {
        return new ReportChatMessageResponse(
                message.getId(),
                message.getTargetId(),
                message.getRole().name(),
                message.getContent(),
                message.getSourceCards(),
                message.getCreatedAt()
        );
    }
}
