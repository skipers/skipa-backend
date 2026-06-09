package com.skipers.skipa.domain.report.application;

import com.skipers.skipa.domain.patent.application.BusinessPatentAccessValidator;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.domain.ReportStatus;
import com.skipers.skipa.domain.report.dto.response.ReportCreateResponse;
import com.skipers.skipa.domain.report.dto.response.ReportDetailResponse;
import com.skipers.skipa.domain.report.dto.response.ReportStatusResponse;
import com.skipers.skipa.domain.report.exception.ReportException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PatentRepository patentRepository;

    @Mock
    private BusinessPatentAccessValidator businessPatentAccessValidator;

    @Mock
    private ReportGenerationPublisher reportGenerationPublisher;

    @Mock
    private ReportStorageService reportStorageService;

    @InjectMocks
    private ReportService reportService;

    @Test
    void createSavesGeneratingReportAndPublishesMessage() {
        Patent patent = Patent.builder()
                .title("Patent")
                .applicationNumber("APP-1")
                .build();
        ReflectionTestUtils.setField(patent, "id", 10L);
        when(patentRepository.findById(10L)).thenReturn(Optional.of(patent));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 1L);
            return report;
        });

        ReportCreateResponse response = reportService.create(10L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.patentId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("GENERATING");
        verify(reportGenerationPublisher).publish(1L, 10L);
    }

    @Test
    void createRejectsMissingPatentWithoutPublishingMessage() {
        when(patentRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.create(10L))
                .isInstanceOf(com.skipers.skipa.domain.patent.exception.PatentException.class);

        verify(reportRepository, never()).save(any());
        verify(reportGenerationPublisher, never()).publish(any(), any());
    }

    @Test
    void createFailsWhenPublisherFails() {
        Patent patent = Patent.builder()
                .title("Patent")
                .applicationNumber("APP-1")
                .build();
        ReflectionTestUtils.setField(patent, "id", 10L);
        when(patentRepository.findById(10L)).thenReturn(Optional.of(patent));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 1L);
            return report;
        });
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(reportGenerationPublisher).publish(1L, 10L);

        assertReportError(() -> reportService.create(10L), ErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    @Test
    void completeStoresReportKeyAndMarksReportCompleted() {
        Report report = report(1L, 10L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        ReportStatusResponse response = reportService.complete(1L, "reports/1/report.html", new BigDecimal("82.50"), "A");

        assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETED);
        assertThat(report.getReportKey()).isEqualTo("reports/1/report.html");
        assertThat(report.getTotalScore()).isEqualByComparingTo("82.50");
        assertThat(report.getValueGrade()).isEqualTo("A");
        assertThat(report.getEvaluatedAt()).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.patentId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.totalScore()).isEqualByComparingTo("82.50");
        assertThat(response.valueGrade()).isEqualTo("A");
    }

    @Test
    void completeRejectsMissingReport() {
        when(reportRepository.findById(1L)).thenReturn(Optional.empty());

        assertReportError(
                () -> reportService.complete(1L, "reports/1/report.html", new BigDecimal("82.50"), "A"),
                ErrorCode.REPORT_NOT_FOUND
        );
    }

    @Test
    void failMarksReportFailed() {
        Report report = report(1L, 10L);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        ReportStatusResponse response = reportService.fail(1L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(report.getReportKey()).isNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.patentId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("FAILED");
    }

    @Test
    void failRejectsMissingReport() {
        when(reportRepository.findById(1L)).thenReturn(Optional.empty());

        assertReportError(() -> reportService.fail(1L), ErrorCode.REPORT_NOT_FOUND);
    }

    @Test
    void getReturnsPresignedUrlWithoutExposingReportKey() {
        Report report = report(1L, 10L);
        report.complete("reports/1/report.html", new BigDecimal("82.50"), "A", null);
        when(reportRepository.findByIdAndPatentId(1L, 10L)).thenReturn(Optional.of(report));
        when(reportStorageService.generatePresignedUrl("reports/1/report.html"))
                .thenReturn("https://minio.example.com/skipa/reports/1/report.html?X-Amz-Signature=abc");

        ReportDetailResponse response = reportService.get(null, 10L, 1L);

        verify(businessPatentAccessValidator).validate(null, 10L);
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.patentId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.totalScore()).isEqualByComparingTo("82.50");
        assertThat(response.valueGrade()).isEqualTo("A");
        assertThat(response.url()).isEqualTo("https://minio.example.com/skipa/reports/1/report.html?X-Amz-Signature=abc");
        assertThat(ReportDetailResponse.class.getRecordComponents())
                .extracting(recordComponent -> recordComponent.getName())
                .doesNotContain("reportKey");
    }

    @Test
    void getRejectsReportThatIsNotCompleted() {
        Report report = report(1L, 10L);
        when(reportRepository.findByIdAndPatentId(1L, 10L)).thenReturn(Optional.of(report));

        assertReportError(() -> reportService.get(null, 10L, 1L), ErrorCode.REPORT_NOT_COMPLETED);

        verify(businessPatentAccessValidator).validate(null, 10L);
        verify(reportStorageService, never()).generatePresignedUrl(any());
    }

    @Test
    void getLatestReturnsGeneratingReportWithoutUrl() {
        Report report = report(1L, 10L);
        when(patentRepository.existsById(10L)).thenReturn(true);
        when(reportRepository.findFirstByPatentIdOrderByIdDesc(10L)).thenReturn(Optional.of(report));

        ReportDetailResponse response = reportService.getLatest(null, 10L);

        verify(businessPatentAccessValidator).validate(null, 10L);
        verify(reportStorageService, never()).generatePresignedUrl(any());
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.patentId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("GENERATING");
        assertThat(response.url()).isNull();
    }

    @Test
    void getLatestReturnsCompletedReportWithPresignedUrl() {
        Report report = report(1L, 10L);
        report.complete("reports/1/report.json", new BigDecimal("82.50"), "A", null);
        when(patentRepository.existsById(10L)).thenReturn(true);
        when(reportRepository.findFirstByPatentIdOrderByIdDesc(10L)).thenReturn(Optional.of(report));
        when(reportStorageService.generatePresignedUrl("reports/1/report.json"))
                .thenReturn("https://minio.example.com/reports/1/report.json?signature=abc");

        ReportDetailResponse response = reportService.getLatest(null, 10L);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.totalScore()).isEqualByComparingTo("82.50");
        assertThat(response.valueGrade()).isEqualTo("A");
        assertThat(response.url()).isEqualTo("https://minio.example.com/reports/1/report.json?signature=abc");
    }

    @Test
    void getLatestRejectsMissingReport() {
        when(patentRepository.existsById(10L)).thenReturn(true);
        when(reportRepository.findFirstByPatentIdOrderByIdDesc(10L)).thenReturn(Optional.empty());

        assertReportError(() -> reportService.getLatest(null, 10L), ErrorCode.REPORT_NOT_FOUND);
    }

    private Report report(Long reportId, Long patentId) {
        Patent patent = Patent.builder()
                .title("Patent")
                .applicationNumber("APP-1")
                .build();
        ReflectionTestUtils.setField(patent, "id", patentId);

        Report report = Report.builder()
                .patent(patent)
                .build();
        ReflectionTestUtils.setField(report, "id", reportId);
        return report;
    }

    private void assertReportError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ReportException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
