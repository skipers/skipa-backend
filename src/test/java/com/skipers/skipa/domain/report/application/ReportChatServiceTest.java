package com.skipers.skipa.domain.report.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.chat.dao.ChatMessageRepository;
import com.skipers.skipa.domain.chat.domain.ChatMessage;
import com.skipers.skipa.domain.chat.domain.ChatRole;
import com.skipers.skipa.domain.chat.domain.ChatSourceCard;
import com.skipers.skipa.domain.chat.domain.ChatTargetType;
import com.skipers.skipa.domain.chat.application.ChatStreamEvent;
import com.skipers.skipa.domain.chat.dto.ChatClientResult;
import com.skipers.skipa.domain.patent.application.ApprovedPatentValidator;
import com.skipers.skipa.domain.patent.application.BusinessPatentAccessValidator;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.dto.request.ReportChatClientRequest;
import com.skipers.skipa.domain.report.dto.request.ReportChatMessageRequest;
import com.skipers.skipa.domain.report.dto.response.ReportChatMessageResponse;
import com.skipers.skipa.domain.report.dto.response.ReportChatSendResponse;
import com.skipers.skipa.domain.report.exception.ReportException;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.domain.user.domain.UserStatus;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportChatServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private BusinessPatentAccessValidator businessPatentAccessValidator;

    @Mock
    private ApprovedPatentValidator approvedPatentValidator;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ReportChatClient chatClient;

    @Mock
    private ReportChatStreamClient chatStreamClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ReportChatService chatService;

    private User user;
    private Patent patent;
    private Report report;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .loginId("legal")
                .name("Legal User")
                .email("legal@example.com")
                .password("password")
                .role(UserRole.LEGAL)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 10L);

        patent = Patent.builder()
                .title("Patent")
                .applicationNumber("APP-1")
                .build();
        ReflectionTestUtils.setField(patent, "id", 100L);

        report = Report.builder()
                .patent(patent)
                .build();
        ReflectionTestUtils.setField(report, "id", 1L);
        report.completeReport("reports/1/report.json", new BigDecimal("82.50"), "A", null);
    }

    @Test
    void getMessagesReturnsOrderedReportMessages() {
        when(approvedPatentValidator.getApprovedPatent(100L)).thenReturn(patent);
        when(reportRepository.findByIdAndPatentId(1L, 100L)).thenReturn(Optional.of(report));
        when(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(ChatTargetType.REPORT, 100L))
                .thenReturn(List.of(message(1000L, ChatRole.USER, "question")));

        List<ReportChatMessageResponse> responses = chatService.getMessages(user, 100L, 1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1000L);
        assertThat(responses.get(0).patentId()).isEqualTo(100L);
        assertThat(responses.get(0).role()).isEqualTo("USER");
        assertThat(responses.get(0).content()).isEqualTo("question");
        verify(businessPatentAccessValidator).validate(user, 100L);
    }

    @Test
    void sendMessageStoresUserMessageAndAssistantMessage() {
        ChatMessage previousQuestion = message(998L, ChatRole.USER, "previous question");
        ChatMessage previousAnswer = message(999L, ChatRole.ASSISTANT, "previous answer");
        when(approvedPatentValidator.getApprovedPatent(100L)).thenReturn(patent);
        when(reportRepository.findByIdAndPatentId(1L, 100L)).thenReturn(Optional.of(report));
        when(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(ChatTargetType.REPORT, 100L))
                .thenReturn(List.of(previousQuestion, previousAnswer));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", message.getRole() == ChatRole.USER ? 1000L : 1001L);
            return message;
        });
        ChatSourceCard sourceCard = new ChatSourceCard(
                "S1",
                "Claim 1",
                "Claim 1",
                "report",
                1,
                "https://example.com/report",
                "p.1",
                "reports/1/report.json",
                List.of("risk"),
                "claim risk"
        );
        when(chatClient.send(any(ReportChatClientRequest.class)))
                .thenReturn(new ChatClientResult("assistant answer", List.of(sourceCard)));

        ReportChatSendResponse response = chatService.sendMessage(
                user,
                100L,
                1L,
                new ReportChatMessageRequest("What is the key risk?")
        );

        assertThat(response.userMessage().id()).isEqualTo(1000L);
        assertThat(response.userMessage().role()).isEqualTo("USER");
        assertThat(response.assistantMessage().id()).isEqualTo(1001L);
        assertThat(response.assistantMessage().role()).isEqualTo("ASSISTANT");
        assertThat(response.assistantMessage().content()).isEqualTo("assistant answer");
        assertThat(response.assistantMessage().sourceCards()).containsExactly(sourceCard);

        ArgumentCaptor<ReportChatClientRequest> requestCaptor = ArgumentCaptor.forClass(ReportChatClientRequest.class);
        verify(chatClient).send(requestCaptor.capture());
        assertThat(requestCaptor.getValue().patentId()).isEqualTo(100L);
        assertThat(requestCaptor.getValue().userId()).isEqualTo("10");
        assertThat(requestCaptor.getValue().question()).isEqualTo("What is the key risk?");
        assertThat(requestCaptor.getValue().chatHistory())
                .containsExactly(new ReportChatClientRequest.History("previous question", "previous answer"));
    }

    @Test
    void sendMessagePassesOnlyFiveRecentHistoryPairs() {
        List<ChatMessage> previousMessages = new java.util.ArrayList<>();
        for (long i = 1; i <= 6; i++) {
            previousMessages.add(message(i * 2 - 1, ChatRole.USER, "question " + i));
            previousMessages.add(message(i * 2, ChatRole.ASSISTANT, "answer " + i));
        }
        when(approvedPatentValidator.getApprovedPatent(100L)).thenReturn(patent);
        when(reportRepository.findByIdAndPatentId(1L, 100L)).thenReturn(Optional.of(report));
        when(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(ChatTargetType.REPORT, 100L))
                .thenReturn(previousMessages);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatClient.send(any(ReportChatClientRequest.class)))
                .thenReturn(ChatClientResult.answerOnly("assistant answer"));

        chatService.sendMessage(user, 100L, 1L, new ReportChatMessageRequest("current question"));

        ArgumentCaptor<ReportChatClientRequest> requestCaptor = ArgumentCaptor.forClass(ReportChatClientRequest.class);
        verify(chatClient).send(requestCaptor.capture());
        assertThat(requestCaptor.getValue().chatHistory())
                .containsExactly(
                        new ReportChatClientRequest.History("question 2", "answer 2"),
                        new ReportChatClientRequest.History("question 3", "answer 3"),
                        new ReportChatClientRequest.History("question 4", "answer 4"),
                        new ReportChatClientRequest.History("question 5", "answer 5"),
                        new ReportChatClientRequest.History("question 6", "answer 6")
                );
    }

    @Test
    void streamMessageProxiesEventsAndStoresAssistantMessageOnDone() throws Exception {
        ChatMessage previousQuestion = message(998L, ChatRole.USER, "previous question");
        ChatMessage previousAnswer = message(999L, ChatRole.ASSISTANT, "previous answer");
        when(approvedPatentValidator.getApprovedPatent(100L)).thenReturn(patent);
        when(reportRepository.findByIdAndPatentId(1L, 100L)).thenReturn(Optional.of(report));
        when(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(ChatTargetType.REPORT, 100L))
                .thenReturn(List.of(previousQuestion, previousAnswer));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
            ReportChatClientRequest request = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Consumer<ChatStreamEvent> consumer = invocation.getArgument(1);
            consumer.accept(streamEvent("metadata", "{\"query\":\"" + request.question() + "\",\"patent_id\":\"100\",\"stream\":true}"));
            consumer.accept(streamEvent("delta", "{\"text\":\"assistant \"}"));
            consumer.accept(streamEvent("done", "{\"query\":\"current question\",\"patent_id\":\"100\",\"answer\":\"assistant answer\",\"source_cards\":[],\"metrics\":{\"stream\":true},\"stream\":true}"));
            return null;
        }).when(chatStreamClient).stream(any(ReportChatClientRequest.class), any());

        StreamingResponseBody responseBody = chatService.streamMessage(
                user,
                100L,
                1L,
                new ReportChatMessageRequest("current question")
        );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        responseBody.writeTo(outputStream);

        String streamed = outputStream.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(streamed).contains("event: metadata");
        assertThat(streamed).contains("event: delta");
        assertThat(streamed).contains("event: done");

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, org.mockito.Mockito.times(2)).save(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(0).getRole()).isEqualTo(ChatRole.USER);
        ChatMessage assistantMessage = messageCaptor.getAllValues().get(1);
        assertThat(assistantMessage.getRole()).isEqualTo(ChatRole.ASSISTANT);
        assertThat(assistantMessage.getContent()).isEqualTo("assistant answer");
    }

    @Test
    void sendMessageRejectsGeneratingReport() {
        Report generatingReport = Report.builder()
                .patent(patent)
                .build();
        ReflectionTestUtils.setField(generatingReport, "id", 2L);
        when(approvedPatentValidator.getApprovedPatent(100L)).thenReturn(patent);
        when(reportRepository.findByIdAndPatentId(2L, 100L)).thenReturn(Optional.of(generatingReport));

        assertReportError(
                () -> chatService.sendMessage(user, 100L, 2L, new ReportChatMessageRequest("question")),
                ErrorCode.REPORT_NOT_COMPLETED
        );
    }

    @Test
    void clearMessagesDeletesReportMessages() {
        when(approvedPatentValidator.getApprovedPatent(100L)).thenReturn(patent);
        when(reportRepository.findByIdAndPatentId(1L, 100L)).thenReturn(Optional.of(report));

        chatService.clearMessages(user, 100L, 1L);

        verify(chatMessageRepository).deleteAllByTargetTypeAndTargetId(ChatTargetType.REPORT, 100L);
    }

    private ChatMessage message(Long id, ChatRole role, String content) {
        ChatMessage message = ChatMessage.builder()
                .targetType(ChatTargetType.REPORT)
                .targetId(patent.getId())
                .user(user)
                .role(role)
                .content(content)
                .build();
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    private ChatStreamEvent streamEvent(String event, String data) throws Exception {
        return new ChatStreamEvent(
                event,
                objectMapper.readTree(data),
                "event: " + event + "\n" + "data: " + data + "\n\n"
        );
    }

    private void assertReportError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ReportException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
