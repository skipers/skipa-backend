package com.skipers.skipa.domain.preevaluation.domain;

import com.skipers.skipa.domain.preevaluation.exception.PreEvaluationException;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreEvaluationTest {

    @Test
    void completeStoresReportKeyAndCompletionTime() {
        PreEvaluation preEvaluation = processingPreEvaluation();
        Instant completedAt = Instant.parse("2026-06-10T08:00:00Z");

        preEvaluation.complete("pre-evaluations/1/report.html", completedAt);

        assertThat(preEvaluation.getStatus()).isEqualTo(PreEvaluationStatus.COMPLETED);
        assertThat(preEvaluation.getReportKey()).isEqualTo("pre-evaluations/1/report.html");
        assertThat(preEvaluation.getCompletedAt()).isEqualTo(completedAt);
        assertThat(preEvaluation.isCompleted()).isTrue();
    }

    @Test
    void completeRejectsBlankReportKey() {
        PreEvaluation preEvaluation = processingPreEvaluation();

        assertPreEvaluationError(
                () -> preEvaluation.complete(" ", Instant.parse("2026-06-10T08:00:00Z")),
                ErrorCode.INVALID_REQUEST
        );

        assertThat(preEvaluation.getStatus()).isEqualTo(PreEvaluationStatus.PROCESSING);
        assertThat(preEvaluation.getReportKey()).isNull();
    }

    @Test
    void failChangesStatus() {
        PreEvaluation preEvaluation = processingPreEvaluation();

        preEvaluation.fail();

        assertThat(preEvaluation.getStatus()).isEqualTo(PreEvaluationStatus.FAILED);
        assertThat(preEvaluation.getReportKey()).isNull();
        assertThat(preEvaluation.getCompletedAt()).isNotNull();
        assertThat(preEvaluation.isCompleted()).isFalse();
    }

    @Test
    void finalizedPreEvaluationCannotBeCompletedAgain() {
        PreEvaluation preEvaluation = processingPreEvaluation();
        preEvaluation.complete("pre-evaluations/1/report.html", null);

        assertPreEvaluationError(
                () -> preEvaluation.complete("pre-evaluations/1/retry.html", null),
                ErrorCode.PRE_EVALUATION_ALREADY_PROCESSED
        );

        assertThat(preEvaluation.getReportKey()).isEqualTo("pre-evaluations/1/report.html");
    }

    @Test
    void finalizedPreEvaluationCannotBeFailedAgain() {
        PreEvaluation preEvaluation = processingPreEvaluation();
        preEvaluation.fail();

        assertPreEvaluationError(preEvaluation::fail, ErrorCode.PRE_EVALUATION_ALREADY_PROCESSED);

        assertThat(preEvaluation.getStatus()).isEqualTo(PreEvaluationStatus.FAILED);
    }

    private PreEvaluation processingPreEvaluation() {
        return PreEvaluation.builder()
                .user(User.builder()
                        .loginId("business")
                        .name("Business User")
                        .email("business@example.com")
                        .password("password")
                        .build())
                .title("Battery safety system")
                .technicalDescription("Detects battery thermal runaway early.")
                .claims(List.of("A battery safety system comprising a sensor unit."))
                .relatedBusiness("EV battery")
                .targetCountries("Korea, United States")
                .build();
    }

    private void assertPreEvaluationError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(PreEvaluationException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
