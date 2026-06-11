package com.skipers.skipa.domain.report.application;

import com.skipers.skipa.domain.chat.dao.ChatMessageRepository;
import com.skipers.skipa.domain.chat.domain.ChatMessage;
import com.skipers.skipa.domain.chat.domain.ChatRole;
import com.skipers.skipa.domain.chat.domain.ChatTargetType;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
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
        report.complete("reports/1/report.json", new BigDecimal("82.50"), "A", null);
    }

    @Test
    void getMessagesReturnsOrderedReportMessages() {
        when(approvedPatentValidator.getApprovedPatent(100L)).thenReturn(patent);
        when(reportRepository.findByIdAndPatentId(1L, 100L)).thenReturn(Optional.of(report));
        when(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(ChatTargetType.REPORT, 1L))
                .thenReturn(List.of(message(1000L, ChatRole.USER, "question")));

        List<ReportChatMessageResponse> responses = chatService.getMessages(user, 100L, 1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1000L);
        assertThat(responses.get(0).reportId()).isEqualTo(1L);
        assertThat(responses.get(0).role()).isEqualTo("USER");
        assertThat(responses.get(0).content()).isEqualTo("question");
        verify(businessPatentAccessValidator).validate(user, 100L);
    }

    @Test
    void sendMessageStoresUserMessageAndAssistantMessage() {
        ChatMessage previousMessage = message(999L, ChatRole.ASSISTANT, "previous answer");
        when(approvedPatentValidator.getApprovedPatent(100L)).thenReturn(patent);
        when(reportRepository.findByIdAndPatentId(1L, 100L)).thenReturn(Optional.of(report));
        when(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(ChatTargetType.REPORT, 1L))
                .thenReturn(List.of(previousMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", message.getRole() == ChatRole.USER ? 1000L : 1001L);
            return message;
        });
        when(chatClient.send(any(ReportChatClientRequest.class))).thenReturn("assistant answer");

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

        ArgumentCaptor<ReportChatClientRequest> requestCaptor = ArgumentCaptor.forClass(ReportChatClientRequest.class);
        verify(chatClient).send(requestCaptor.capture());
        assertThat(requestCaptor.getValue().reportId()).isEqualTo(1L);
        assertThat(requestCaptor.getValue().patentId()).isEqualTo(100L);
        assertThat(requestCaptor.getValue().userId()).isEqualTo(10L);
        assertThat(requestCaptor.getValue().reportKey()).isEqualTo("reports/1/report.json");
        assertThat(requestCaptor.getValue().message()).isEqualTo("What is the key risk?");
        assertThat(requestCaptor.getValue().history())
                .extracting(ReportChatClientRequest.Message::content)
                .containsExactly("previous answer", "What is the key risk?");
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

        verify(chatMessageRepository).deleteAllByTargetTypeAndTargetId(ChatTargetType.REPORT, 1L);
    }

    private ChatMessage message(Long id, ChatRole role, String content) {
        ChatMessage message = ChatMessage.builder()
                .targetType(ChatTargetType.REPORT)
                .targetId(report.getId())
                .user(user)
                .role(role)
                .content(content)
                .build();
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    private void assertReportError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ReportException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
