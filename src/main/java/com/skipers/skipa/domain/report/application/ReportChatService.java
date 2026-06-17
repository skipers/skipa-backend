package com.skipers.skipa.domain.report.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.chat.application.ChatStreamEvent;
import com.skipers.skipa.domain.chat.application.ChatStreamWriter;
import com.skipers.skipa.domain.chat.dao.ChatMessageRepository;
import com.skipers.skipa.domain.chat.domain.ChatMessage;
import com.skipers.skipa.domain.chat.domain.ChatRole;
import com.skipers.skipa.domain.chat.domain.ChatTargetType;
import com.skipers.skipa.domain.chat.dto.ChatClientResult;
import com.skipers.skipa.domain.patent.application.ApprovedPatentValidator;
import com.skipers.skipa.domain.patent.application.BusinessPatentAccessValidator;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.dto.request.ReportChatClientRequest;
import com.skipers.skipa.domain.report.dto.request.ReportChatMessageRequest;
import com.skipers.skipa.domain.report.dto.response.ReportChatClientResponse;
import com.skipers.skipa.domain.report.dto.response.ReportChatMessageResponse;
import com.skipers.skipa.domain.report.dto.response.ReportChatSendResponse;
import com.skipers.skipa.domain.report.exception.ReportException;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
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
    private final ReportChatStreamClient chatStreamClient;
    private final ObjectMapper objectMapper;

    public List<ReportChatMessageResponse> getMessages(User user, Long patentId, Long reportId) {
        Report report = getAccessibleCompletedReport(user, patentId, reportId);

        return chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                        ChatTargetType.REPORT,
                        report.getPatent().getId()
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
                report.getPatent().getId()
        );

        ChatMessage userMessage = chatMessageRepository.save(ChatMessage.builder()
                .targetType(ChatTargetType.REPORT)
                .targetId(report.getPatent().getId())
                .user(user)
                .role(ChatRole.USER)
                .content(request.message())
                .build());

        ChatClientResult aiResult = sendToAiServer(report, user.getId(), request.message(), toRecentHistory(previousMessages));
        ChatMessage assistantMessage = chatMessageRepository.save(ChatMessage.builder()
                .targetType(ChatTargetType.REPORT)
                .targetId(report.getPatent().getId())
                .user(user)
                .role(ChatRole.ASSISTANT)
                .content(aiResult.answer())
                .sourceCards(aiResult.sourceCards())
                .build());

        return ReportChatSendResponse.of(userMessage, assistantMessage);
    }

    @Transactional
    public StreamingResponseBody streamMessage(
            User user,
            Long patentId,
            Long reportId,
            ReportChatMessageRequest request
    ) {
        Report report = getAccessibleCompletedReport(user, patentId, reportId);
        List<ChatMessage> previousMessages = chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                ChatTargetType.REPORT,
                report.getPatent().getId()
        );

        chatMessageRepository.save(ChatMessage.builder()
                .targetType(ChatTargetType.REPORT)
                .targetId(report.getPatent().getId())
                .user(user)
                .role(ChatRole.USER)
                .content(request.message())
                .build());

        ReportChatClientRequest clientRequest = ReportChatClientRequest.of(
                report,
                user.getId(),
                request.message(),
                toRecentHistory(previousMessages)
        );

        return outputStream -> streamToClient(outputStream, user, report.getPatent().getId(), clientRequest);
    }

    @Transactional
    public void clearMessages(User user, Long patentId, Long reportId) {
        Report report = getAccessibleCompletedReport(user, patentId, reportId);
        chatMessageRepository.deleteAllByTargetTypeAndTargetId(ChatTargetType.REPORT, report.getPatent().getId());
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

    private ChatClientResult sendToAiServer(
            Report report,
            Long userId,
            String message,
            List<ReportChatClientRequest.History> history
    ) {
        try {
            return chatClient.send(ReportChatClientRequest.of(report, userId, message, history));
        } catch (RuntimeException e) {
            throw new ReportException(ErrorCode.AI_SERVER_ERROR, e);
        }
    }

    private void streamToClient(
            OutputStream outputStream,
            User user,
            Long targetId,
            ReportChatClientRequest clientRequest
    ) throws IOException {
        boolean[] terminalReceived = {false};
        try {
            chatStreamClient.stream(clientRequest, event -> {
                try {
                    if ("done".equals(event.event())) {
                        saveAssistantMessage(user, targetId, event);
                        terminalReceived[0] = true;
                    } else if ("error".equals(event.event())) {
                        terminalReceived[0] = true;
                    }
                    ChatStreamWriter.write(outputStream, event.raw());
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to write chat stream event", e);
                }
            });
            if (!terminalReceived[0]) {
                ChatStreamWriter.writeError(outputStream, objectMapper, "AI stream ended before a done event was received.");
            }
        } catch (RuntimeException e) {
            ChatStreamWriter.writeError(outputStream, objectMapper, e.getMessage());
        }
    }

    private void saveAssistantMessage(User user, Long targetId, ChatStreamEvent event) {
        ReportChatClientResponse response = objectMapper.convertValue(event.data(), ReportChatClientResponse.class);
        if (response == null || !StringUtils.hasText(response.answer())) {
            throw new ReportException(ErrorCode.AI_SERVER_ERROR);
        }

        chatMessageRepository.save(ChatMessage.builder()
                .targetType(ChatTargetType.REPORT)
                .targetId(targetId)
                .user(user)
                .role(ChatRole.ASSISTANT)
                .content(response.answer())
                .sourceCards(response.toResult().sourceCards())
                .build());
    }

    private List<ReportChatClientRequest.History> toRecentHistory(List<ChatMessage> messages) {
        List<ReportChatClientRequest.History> histories = new java.util.ArrayList<>();
        String pendingQuestion = null;

        for (ChatMessage message : messages) {
            if (message.getRole() == ChatRole.USER) {
                pendingQuestion = message.getContent();
            } else if (message.getRole() == ChatRole.ASSISTANT && pendingQuestion != null) {
                histories.add(new ReportChatClientRequest.History(pendingQuestion, message.getContent()));
                pendingQuestion = null;
            }
        }

        int fromIndex = Math.max(0, histories.size() - 5);
        return histories.subList(fromIndex, histories.size());
    }
}
