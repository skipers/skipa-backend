package com.skipers.skipa.domain.report.application;

import com.skipers.skipa.domain.chat.dao.ChatMessageRepository;
import com.skipers.skipa.domain.chat.domain.ChatMessage;
import com.skipers.skipa.domain.chat.domain.ChatRole;
import com.skipers.skipa.domain.chat.domain.ChatTargetType;
import com.skipers.skipa.domain.patent.application.ApprovedPatentValidator;
import com.skipers.skipa.domain.patent.application.BusinessPatentAccessValidator;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.dto.request.ReportChatClientRequest;
import com.skipers.skipa.domain.report.dto.request.ReportChatMessageRequest;
import com.skipers.skipa.domain.report.dto.response.ReportChatMessageResponse;
import com.skipers.skipa.domain.report.dto.response.ReportChatSendResponse;
import com.skipers.skipa.domain.report.exception.ReportException;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportChatService {

    private final ReportRepository reportRepository;
    private final BusinessPatentAccessValidator businessPatentAccessValidator;
    private final ApprovedPatentValidator approvedPatentValidator;
    private final ChatMessageRepository chatMessageRepository;
    private final ReportChatClient chatClient;

    public List<ReportChatMessageResponse> getMessages(User user, Long patentId, Long reportId) {
        Report report = getAccessibleCompletedReport(user, patentId, reportId);

        return chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                        ChatTargetType.REPORT,
                        report.getId()
                )
                .stream()
                .map(ReportChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public ReportChatSendResponse sendMessage(
            User user,
            Long patentId,
            Long reportId,
            ReportChatMessageRequest request
    ) {
        Report report = getAccessibleCompletedReport(user, patentId, reportId);
        List<ChatMessage> previousMessages = chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                ChatTargetType.REPORT,
                report.getId()
        );

        ChatMessage userMessage = chatMessageRepository.save(ChatMessage.builder()
                .targetType(ChatTargetType.REPORT)
                .targetId(report.getId())
                .user(user)
                .role(ChatRole.USER)
                .content(request.message())
                .build());

        List<ReportChatClientRequest.Message> history = new ArrayList<>(previousMessages.stream()
                .map(ReportChatClientRequest.Message::from)
                .toList());
        history.add(ReportChatClientRequest.Message.from(userMessage));

        String assistantContent = sendToAiServer(report, user.getId(), request.message(), history);
        ChatMessage assistantMessage = chatMessageRepository.save(ChatMessage.builder()
                .targetType(ChatTargetType.REPORT)
                .targetId(report.getId())
                .user(user)
                .role(ChatRole.ASSISTANT)
                .content(assistantContent)
                .build());

        return ReportChatSendResponse.of(userMessage, assistantMessage);
    }

    @Transactional
    public void clearMessages(User user, Long patentId, Long reportId) {
        Report report = getAccessibleCompletedReport(user, patentId, reportId);
        chatMessageRepository.deleteAllByTargetTypeAndTargetId(ChatTargetType.REPORT, report.getId());
    }

    private Report getAccessibleCompletedReport(User user, Long patentId, Long reportId) {
        businessPatentAccessValidator.validate(user, patentId);
        approvedPatentValidator.getApprovedPatent(patentId);

        Report report = reportRepository.findByIdAndPatentId(reportId, patentId)
                .orElseThrow(() -> new ReportException(ErrorCode.REPORT_NOT_FOUND));

        if (!report.isReportGenerated()) {
            throw new ReportException(ErrorCode.REPORT_NOT_COMPLETED);
        }

        return report;
    }

    private String sendToAiServer(
            Report report,
            Long userId,
            String message,
            List<ReportChatClientRequest.Message> history
    ) {
        try {
            return chatClient.send(ReportChatClientRequest.of(report, userId, message, history));
        } catch (RuntimeException e) {
            throw new ReportException(ErrorCode.AI_SERVER_ERROR, e);
        }
    }
}
