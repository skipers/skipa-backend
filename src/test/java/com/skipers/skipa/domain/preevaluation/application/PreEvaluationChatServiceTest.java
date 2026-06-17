package com.skipers.skipa.domain.preevaluation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.chat.application.ChatStreamEvent;
import com.skipers.skipa.domain.chat.dao.ChatMessageRepository;
import com.skipers.skipa.domain.chat.domain.ChatMessage;
import com.skipers.skipa.domain.chat.domain.ChatRole;
import com.skipers.skipa.domain.chat.domain.ChatSourceCard;
import com.skipers.skipa.domain.chat.domain.ChatTargetType;
import com.skipers.skipa.domain.chat.dto.ChatClientResult;
import com.skipers.skipa.domain.preevaluation.dao.PreEvaluationRepository;
import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationChatClientRequest;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationChatMessageRequest;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationChatMessageResponse;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationChatSendResponse;
import com.skipers.skipa.domain.preevaluation.exception.PreEvaluationException;
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
class PreEvaluationChatServiceTest {

    @Mock
    private PreEvaluationRepository preEvaluationRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private PreEvaluationChatClient chatClient;

    @Mock
    private PreEvaluationChatStreamClient chatStreamClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PreEvaluationChatService chatService;

    private User user;
    private PreEvaluation preEvaluation;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .loginId("business")
                .name("Business User")
                .email("business@example.com")
                .password("password")
                .role(UserRole.BUSINESS)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 10L);

        preEvaluation = PreEvaluation.builder()
                .user(user)
                .title("Battery safety system")
                .technicalDescription("Detects battery thermal runaway early.")
                .claims(List.of("A battery safety system comprising a sensor unit."))
                .relatedBusiness("EV battery")
                .targetCountries("Korea, United States")
                .build();
        ReflectionTestUtils.setField(preEvaluation, "id", 1L);
    }

    @Test
    void getMessagesReturnsOrderedMessages() {
        when(preEvaluationRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(preEvaluation));
        when(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(ChatTargetType.PRE_EVALUATION, 1L))
                .thenReturn(List.of(message(100L, ChatRole.USER, "question")));

        List<PreEvaluationChatMessageResponse> responses = chatService.getMessages(user, 1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(100L);
        assertThat(responses.get(0).role()).isEqualTo("USER");
        assertThat(responses.get(0).content()).isEqualTo("question");
    }

    @Test
    void sendMessageStoresUserMessageAndAssistantMessage() {
        ChatMessage previousQuestion = message(98L, ChatRole.USER, "previous question");
        ChatMessage previousAnswer = message(99L, ChatRole.ASSISTANT, "previous answer");
        when(preEvaluationRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(preEvaluation));
        when(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(ChatTargetType.PRE_EVALUATION, 1L))
                .thenReturn(List.of(previousQuestion, previousAnswer));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id",
                    message.getRole() == ChatRole.USER ? 100L : 101L);
            return message;
        });
        ChatSourceCard sourceCard = new ChatSourceCard(
                "S1",
                "Pre-evaluation report",
                "Pre-evaluation report",
                "pre_evaluation",
                1,
                "https://example.com/pre-evaluation",
                "section 1",
                "pre-evaluations/1/report.json",
                List.of("claim"),
                "claim suggestion"
        );
        when(chatClient.send(any(PreEvaluationChatClientRequest.class)))
                .thenReturn(new ChatClientResult("assistant answer", List.of(sourceCard)));

        PreEvaluationChatSendResponse response = chatService.sendMessage(
                user,
                1L,
                new PreEvaluationChatMessageRequest("How can I improve this patent?")
        );

        assertThat(response.userMessage().id()).isEqualTo(100L);
        assertThat(response.userMessage().preEvaluationId()).isEqualTo(1L);
        assertThat(response.userMessage().role()).isEqualTo("USER");
        assertThat(response.assistantMessage().id()).isEqualTo(101L);
        assertThat(response.assistantMessage().preEvaluationId()).isEqualTo(1L);
        assertThat(response.assistantMessage().role()).isEqualTo("ASSISTANT");
        assertThat(response.assistantMessage().content()).isEqualTo("assistant answer");
        assertThat(response.assistantMessage().sourceCards()).containsExactly(sourceCard);

        ArgumentCaptor<PreEvaluationChatClientRequest> requestCaptor =
                ArgumentCaptor.forClass(PreEvaluationChatClientRequest.class);
        verify(chatClient).send(requestCaptor.capture());
        assertThat(requestCaptor.getValue().caseId()).isEqualTo(1L);
        assertThat(requestCaptor.getValue().userId()).isEqualTo("10");
        assertThat(requestCaptor.getValue().question()).isEqualTo("How can I improve this patent?");
        assertThat(requestCaptor.getValue().chatHistory())
                .containsExactly(new PreEvaluationChatClientRequest.History("previous question", "previous answer"));
    }

    @Test
    void sendMessagePassesOnlyFiveRecentHistoryPairs() {
        List<ChatMessage> previousMessages = new java.util.ArrayList<>();
        for (long i = 1; i <= 6; i++) {
            previousMessages.add(message(i * 2 - 1, ChatRole.USER, "question " + i));
            previousMessages.add(message(i * 2, ChatRole.ASSISTANT, "answer " + i));
        }
        when(preEvaluationRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(preEvaluation));
        when(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(ChatTargetType.PRE_EVALUATION, 1L))
                .thenReturn(previousMessages);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatClient.send(any(PreEvaluationChatClientRequest.class)))
                .thenReturn(ChatClientResult.answerOnly("assistant answer"));

        chatService.sendMessage(user, 1L, new PreEvaluationChatMessageRequest("current question"));

        ArgumentCaptor<PreEvaluationChatClientRequest> requestCaptor =
                ArgumentCaptor.forClass(PreEvaluationChatClientRequest.class);
        verify(chatClient).send(requestCaptor.capture());
        assertThat(requestCaptor.getValue().chatHistory())
                .containsExactly(
                        new PreEvaluationChatClientRequest.History("question 2", "answer 2"),
                        new PreEvaluationChatClientRequest.History("question 3", "answer 3"),
                        new PreEvaluationChatClientRequest.History("question 4", "answer 4"),
                        new PreEvaluationChatClientRequest.History("question 5", "answer 5"),
                        new PreEvaluationChatClientRequest.History("question 6", "answer 6")
                );
    }

    @Test
    void streamMessageProxiesEventsAndStoresAssistantMessageOnDone() throws Exception {
        ChatMessage previousQuestion = message(98L, ChatRole.USER, "previous question");
        ChatMessage previousAnswer = message(99L, ChatRole.ASSISTANT, "previous answer");
        when(preEvaluationRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(preEvaluation));
        when(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(ChatTargetType.PRE_EVALUATION, 1L))
                .thenReturn(List.of(previousQuestion, previousAnswer));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
            PreEvaluationChatClientRequest request = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Consumer<ChatStreamEvent> consumer = invocation.getArgument(1);
            consumer.accept(streamEvent("metadata", "{\"query\":\"" + request.question() + "\",\"patent_id\":\"1\",\"case_id\":\"1\",\"stream\":true}"));
            consumer.accept(streamEvent("delta", "{\"text\":\"assistant \"}"));
            consumer.accept(streamEvent("done", "{\"query\":\"current question\",\"patent_id\":\"1\",\"case_id\":\"1\",\"answer\":\"assistant answer\",\"source_cards\":[],\"metrics\":{\"stream\":true},\"stream\":true}"));
            return null;
        }).when(chatStreamClient).stream(any(PreEvaluationChatClientRequest.class), any());

        StreamingResponseBody responseBody = chatService.streamMessage(
                user,
                1L,
                new PreEvaluationChatMessageRequest("current question")
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
    void sendMessageWrapsAiClientFailure() {
        when(preEvaluationRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(preEvaluation));
        when(chatMessageRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(ChatTargetType.PRE_EVALUATION, 1L)).thenReturn(List.of());
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatClient.send(any(PreEvaluationChatClientRequest.class))).thenThrow(new RuntimeException("AI unavailable"));

        assertPreEvaluationError(
                () -> chatService.sendMessage(user, 1L, new PreEvaluationChatMessageRequest("question")),
                ErrorCode.AI_SERVER_ERROR
        );
    }

    @Test
    void clearMessagesDeletesAllMessagesForOwnedPreEvaluation() {
        when(preEvaluationRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(preEvaluation));

        chatService.clearMessages(user, 1L);

        verify(chatMessageRepository).deleteAllByTargetTypeAndTargetId(ChatTargetType.PRE_EVALUATION, 1L);
    }

    @Test
    void getMessagesRejectsOtherUsersPreEvaluation() {
        when(preEvaluationRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.empty());

        assertPreEvaluationError(() -> chatService.getMessages(user, 1L), ErrorCode.PRE_EVALUATION_NOT_FOUND);
    }

    private ChatMessage message(Long id, ChatRole role, String content) {
        ChatMessage message = ChatMessage.builder()
                .targetType(ChatTargetType.PRE_EVALUATION)
                .targetId(preEvaluation.getId())
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

    private void assertPreEvaluationError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(PreEvaluationException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
