package com.skipers.skipa.domain.report.dto.response;

import com.skipers.skipa.domain.chat.domain.ChatMessage;

import java.time.Instant;

public record ReportChatMessageResponse(
        Long id,
        Long reportId,
        String role,
        String content,
        Instant createdAt
) {

    public static ReportChatMessageResponse from(ChatMessage message) {
        return new ReportChatMessageResponse(
                message.getId(),
                message.getTargetId(),
                message.getRole().name(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
