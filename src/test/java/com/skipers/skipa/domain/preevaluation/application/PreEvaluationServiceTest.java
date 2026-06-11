package com.skipers.skipa.domain.preevaluation.application;

import com.skipers.skipa.domain.chat.dao.ChatMessageRepository;
import com.skipers.skipa.domain.chat.domain.ChatTargetType;
import com.skipers.skipa.domain.preevaluation.dao.PreEvaluationRepository;
import com.skipers.skipa.domain.preevaluation.domain.PreEvaluation;
import com.skipers.skipa.domain.preevaluation.domain.PreEvaluationStatus;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationCreateRequest;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationCreateResponse;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationDetailResponse;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationStatusResponse;
import com.skipers.skipa.domain.preevaluation.exception.PreEvaluationException;
import com.skipers.skipa.domain.report.application.ReportStorageService;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.domain.user.domain.UserStatus;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreEvaluationServiceTest {

    @Mock
    private PreEvaluationRepository preEvaluationRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private PreEvaluationGenerationPublisher generationPublisher;

    @Mock
    private ReportStorageService reportStorageService;

    @InjectMocks
    private PreEvaluationService preEvaluationService;

    private User user;

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
    }

    @Test
    void createSavesProcessingPreEvaluationAndPublishesMessage() {
        when(preEvaluationRepository.save(any(PreEvaluation.class))).thenAnswer(invocation -> {
            PreEvaluation preEvaluation = invocation.getArgument(0);
            ReflectionTestUtils.setField(preEvaluation, "id", 1L);
            return preEvaluation;
        });

        PreEvaluationCreateResponse response = preEvaluationService.create(user, createRequest());

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("PROCESSING");
        verify(generationPublisher).publish(any(PreEvaluation.class));
    }

    @Test
    void createFailsWhenPublisherFails() {
        when(preEvaluationRepository.save(any(PreEvaluation.class))).thenAnswer(invocation -> {
            PreEvaluation preEvaluation = invocation.getArgument(0);
            ReflectionTestUtils.setField(preEvaluation, "id", 1L);
            return preEvaluation;
        });
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(generationPublisher).publish(any(PreEvaluation.class));

        assertPreEvaluationError(
                () -> preEvaluationService.create(user, createRequest()),
                ErrorCode.EXTERNAL_SERVICE_ERROR
        );
    }

    @Test
    void completeStoresReportKeyAndMarksCompleted() {
        PreEvaluation preEvaluation = preEvaluation(1L);
        when(preEvaluationRepository.findById(1L)).thenReturn(Optional.of(preEvaluation));

        PreEvaluationStatusResponse response = preEvaluationService.complete(
                1L,
                "pre-evaluations/1/report.json"
        );

        assertThat(preEvaluation.getStatus()).isEqualTo(PreEvaluationStatus.COMPLETED);
        assertThat(preEvaluation.getReportKey()).isEqualTo("pre-evaluations/1/report.json");
        assertThat(preEvaluation.getCompletedAt()).isNotNull();
        assertThat(response.status()).isEqualTo("COMPLETED");
    }

    @Test
    void getCompletedPreEvaluationReturnsGeneratedReportUrl() {
        PreEvaluation preEvaluation = preEvaluation(1L);
        preEvaluation.complete("pre-evaluations/1/report.json", null);
        when(preEvaluationRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(preEvaluation));
        when(reportStorageService.generatePresignedUrl("pre-evaluations/1/report.json"))
                .thenReturn("https://minio.example.com/presigned/pre-evaluations/1/report.json");

        PreEvaluationDetailResponse response = preEvaluationService.get(user, 1L);

        assertThat(response.reportUrl()).isEqualTo("https://minio.example.com/presigned/pre-evaluations/1/report.json");
    }

    @Test
    void failMarksPreEvaluationFailed() {
        PreEvaluation preEvaluation = preEvaluation(1L);
        when(preEvaluationRepository.findById(1L)).thenReturn(Optional.of(preEvaluation));

        PreEvaluationStatusResponse response = preEvaluationService.fail(1L);

        assertThat(preEvaluation.getStatus()).isEqualTo(PreEvaluationStatus.FAILED);
        assertThat(response.status()).isEqualTo("FAILED");
    }

    @Test
    void deleteRemovesChatMessagesBeforePreEvaluation() {
        PreEvaluation preEvaluation = preEvaluation(1L);
        when(preEvaluationRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(preEvaluation));

        preEvaluationService.delete(user, 1L);

        verify(chatMessageRepository).deleteAllByTargetTypeAndTargetId(ChatTargetType.PRE_EVALUATION, 1L);
        verify(preEvaluationRepository).delete(preEvaluation);
    }

    @Test
    void getRejectsOtherUsersPreEvaluation() {
        when(preEvaluationRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.empty());

        assertPreEvaluationError(() -> preEvaluationService.get(user, 1L), ErrorCode.PRE_EVALUATION_NOT_FOUND);
    }

    private PreEvaluationCreateRequest createRequest() {
        return new PreEvaluationCreateRequest(
                "Battery safety system",
                "Detects battery thermal runaway early.",
                List.of("A battery safety system comprising a sensor unit."),
                "EV battery",
                "Korea, United States"
        );
    }

    private PreEvaluation preEvaluation(Long id) {
        PreEvaluation preEvaluation = PreEvaluation.builder()
                .user(user)
                .title("Battery safety system")
                .technicalDescription("Detects battery thermal runaway early.")
                .claims(List.of("A battery safety system comprising a sensor unit."))
                .relatedBusiness("EV battery")
                .targetCountries("Korea, United States")
                .build();
        ReflectionTestUtils.setField(preEvaluation, "id", id);
        return preEvaluation;
    }

    private void assertPreEvaluationError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(PreEvaluationException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
        verify(generationPublisher, never()).publish(null);
    }
}
