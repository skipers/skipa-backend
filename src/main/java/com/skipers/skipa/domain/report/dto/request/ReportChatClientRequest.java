package com.skipers.skipa.domain.report.dto.request;

import com.skipers.skipa.domain.chat.domain.ChatMessage;
import com.skipers.skipa.domain.report.domain.Report;

import java.math.BigDecimal;
import java.util.List;

public record ReportChatClientRequest(
        Long reportId,
        Long patentId,
        Long userId,
        String reportKey,
        BigDecimal totalScore,
        String valueGrade,
        String message,
        List<Message> history
) {

    public static ReportChatClientRequest of(
            Report report,
            Long userId,
            String message,
            List<Message> history
    ) {
        return new ReportChatClientRequest(
                report.getId(),
                report.getPatent().getId(),
                userId,
                report.getReportKey(),
                report.getTotalScore(),
                report.getValueGrade(),
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
