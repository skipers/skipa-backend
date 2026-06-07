package com.skipers.skipa.domain.report.domain;

import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.report.exception.ReportException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportTest {

    @Test
    void completeStoresReportKeyAndCompletionTime() {
        Report report = generatingReport();
        Instant evaluatedAt = Instant.parse("2026-06-07T08:55:00Z");

        report.complete("reports/1/report.html", evaluatedAt);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETED);
        assertThat(report.getReportKey()).isEqualTo("reports/1/report.html");
        assertThat(report.getEvaluatedAt()).isEqualTo(evaluatedAt);
        assertThat(report.isCompleted()).isTrue();
    }

    @Test
    void completeRejectsBlankReportKey() {
        Report report = generatingReport();

        assertReportError(
                () -> report.complete(" ", Instant.parse("2026-06-07T08:55:00Z")),
                ErrorCode.INVALID_REQUEST
        );

        assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATING);
        assertThat(report.getReportKey()).isNull();
    }

    @Test
    void failChangesStatusWithoutReportKey() {
        Report report = generatingReport();

        report.fail();

        assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(report.getReportKey()).isNull();
        assertThat(report.getEvaluatedAt()).isNull();
        assertThat(report.isCompleted()).isFalse();
    }

    @Test
    void finalizedReportCannotBeCompletedAgain() {
        Report report = generatingReport();
        report.complete("reports/1/report.html", Instant.parse("2026-06-07T08:55:00Z"));

        assertReportError(
                () -> report.complete("reports/1/retry.html", Instant.parse("2026-06-07T09:00:00Z")),
                ErrorCode.REPORT_ALREADY_PROCESSED
        );

        assertThat(report.getReportKey()).isEqualTo("reports/1/report.html");
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
