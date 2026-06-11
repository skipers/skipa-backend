package com.skipers.skipa.domain.report.dto.response;

import com.skipers.skipa.domain.chat.domain.ChatMessage;

public record ReportChatSendResponse(
        ReportChatMessageResponse userMessage,
        ReportChatMessageResponse assistantMessage
) {

    public static ReportChatSendResponse of(
            ChatMessage userMessage,
            ChatMessage assistantMessage
    ) {
        return new ReportChatSendResponse(
                ReportChatMessageResponse.from(userMessage),
                ReportChatMessageResponse.from(assistantMessage)
        );
    }
}
