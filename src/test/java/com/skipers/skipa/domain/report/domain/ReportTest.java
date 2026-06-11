package com.skipers.skipa.domain.report.domain;

import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.report.exception.ReportException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportTest {

    @Test
    void completeStoresReportKeyAndCompletionTime() {
        Report report = generatingReport();
        Instant evaluatedAt = Instant.parse("2026-06-07T08:55:00Z");

        report.completeReport("reports/1/report.html", new BigDecimal("82.50"), "A", evaluatedAt);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.REPORT_COMPLETED);
        assertThat(report.getReportKey()).isEqualTo("reports/1/report.html");
        assertThat(report.getTotalScore()).isEqualByComparingTo("82.50");
        assertThat(report.getValueGrade()).isEqualTo("A");
        assertThat(report.getEvaluatedAt()).isEqualTo(evaluatedAt);
        assertThat(report.isReportGenerated()).isTrue();
    }

    @Test
    void completeRejectsBlankReportKey() {
        Report report = generatingReport();

        assertReportError(
                () -> report.completeReport(" ", new BigDecimal("82.50"), "A", Instant.parse("2026-06-07T08:55:00Z")),
                ErrorCode.INVALID_REQUEST
        );

        assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATING);
        assertThat(report.getReportKey()).isNull();
    }

    @Test
    void completeRejectsMissingTotalScore() {
        Report report = generatingReport();

        assertReportError(
                () -> report.completeReport("reports/1/report.html", null, "A", Instant.parse("2026-06-07T08:55:00Z")),
                ErrorCode.INVALID_REQUEST
        );

        assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATING);
        assertThat(report.getTotalScore()).isNull();
    }

    @Test
    void completeRejectsMissingValueGrade() {
        Report report = generatingReport();

        assertReportError(
                () -> report.completeReport("reports/1/report.html", new BigDecimal("82.50"), null, Instant.parse("2026-06-07T08:55:00Z")),
                ErrorCode.INVALID_REQUEST
        );

        assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATING);
        assertThat(report.getValueGrade()).isNull();
    }

    @Test
    void failChangesStatusWithoutReportKey() {
        Report report = generatingReport();

        report.fail();

        assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(report.getReportKey()).isNull();
        assertThat(report.getEvaluatedAt()).isNull();
        assertThat(report.isReportGenerated()).isFalse();
    }

    @Test
    void finalizedReportCannotBeCompletedAgain() {
        Report report = generatingReport();
        report.completeReport("reports/1/report.html", new BigDecimal("82.50"), "A", Instant.parse("2026-06-07T08:55:00Z"));

        assertReportError(
                () -> report.completeReport("reports/1/retry.html", new BigDecimal("83.00"), "A", Instant.parse("2026-06-07T09:00:00Z")),
                ErrorCode.REPORT_ALREADY_PROCESSED
        );

        assertThat(report.getReportKey()).isEqualTo("reports/1/report.html");
    }

    @Test
    void completeEmbeddingMarksEmbeddingCompleted() {
        Report report = generatingReport();
        report.completeReport("reports/1/report.html", new BigDecimal("82.50"), "A", Instant.parse("2026-06-07T08:55:00Z"));

        report.completeEmbedding();

        assertThat(report.getStatus()).isEqualTo(ReportStatus.EMBEDDING_COMPLETED);
        assertThat(report.isReportGenerated()).isTrue();
    }

    @Test
    void completeEmbeddingRejectsReportWithoutGeneratedReport() {
        Report report = generatingReport();

        assertReportError(report::completeEmbedding, ErrorCode.REPORT_ALREADY_PROCESSED);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATING);
    }

    @Test
    void finalizedReportCannotBeFailedAgain() {
        Report report = generatingReport();
        report.fail();

        assertReportError(report::fail, ErrorCode.REPORT_ALREADY_PROCESSED);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
    }

    private Report generatingReport() {
        return Report.builder()
                .patent(Patent.builder()
                        .title("Patent")
                        .applicationNumber("APP-1")
                        .build())
                .build();
    }

    private void assertReportError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ReportException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
