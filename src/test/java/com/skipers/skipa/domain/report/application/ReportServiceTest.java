package com.skipers.skipa.domain.report.application;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.application.ApprovedPatentValidator;
import com.skipers.skipa.domain.patent.application.BusinessPatentAccessValidator;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.portfolio.application.PortfolioInsightCacheInvalidator;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.domain.ReportStatus;
import com.skipers.skipa.domain.report.dto.response.ReportCreateResponse;
import com.skipers.skipa.domain.report.dto.response.ReportDetailResponse;
import com.skipers.skipa.domain.report.dto.response.ReportHistoryResponse;
import com.skipers.skipa.domain.report.dto.response.ReportStatusResponse;
import com.skipers.skipa.domain.report.exception.ReportException;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.BusinessOpinion;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PatentRepository patentRepository;

    @Mock
    private BusinessPatentAccessValidator businessPatentAccessValidator;

    @Mock
    private ApprovedPatentValidator approvedPatentValidator;

    @Mock
    private ReportGenerationPublisher reportGenerationPublisher;

    @Mock
    private ReportStorageService reportStorageService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PortfolioInsightCacheInvalidator portfolioInsightCacheInvalidator;

    @InjectMocks
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        lenient().when(approvedPatentValidator.getApprovedPatent(any()))
                .thenAnswer(invocation -> {
                    Optional<Patent> patent = patentRepository.findById(invocation.getArgument(0));
                    if (patent != null && patent.isPresent()) {
                        return patent.get();
                    }
                    Patent defaultPatent = Patent.builder().title("Patent").applicationNumber("APP-1").build();
                    ReflectionTestUtils.setField(defaultPatent, "id", invocation.getArgument(0));
                    return defaultPatent;
                });
    }

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
        when(approvedPatentValidator.getApprovedPatent(10L))
                .thenThrow(new PatentException(ErrorCode.PATENT_NOT_FOUND));

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

        assertThat(report.getStatus()).isEqualTo(ReportStatus.REPORT_COMPLETED);
        assertThat(report.getReportKey()).isEqualTo("reports/1/report.html");
        assertThat(report.getTotalScore()).isEqualByComparingTo("82.50");
        assertThat(report.getValueGrade()).isEqualTo("A");
        assertThat(report.getEvaluatedAt()).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.patentId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("REPORT_COMPLETED");
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
    void completeEmbeddingMarksReportEmbeddingCompleted() {
        Report report = report(1L, 10L);
        report.completeReport("reports/1/report.html", new BigDecimal("82.50"), "A", null);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        ReportStatusResponse response = reportService.completeEmbedding(1L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.EMBEDDING_COMPLETED);
        assertThat(response.status()).isEqualTo("EMBEDDING_COMPLETED");
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
        report.completeReport("reports/1/report.html", new BigDecimal("82.50"), "A", null);
        Review review = submittedReview(20L, report, BusinessOpinion.MAINTAIN, "유지 의견");
        when(reportRepository.findByIdAndPatentId(1L, 10L)).thenReturn(Optional.of(report));
        when(reportStorageService.generatePresignedUrl("reports/1/report.html"))
                .thenReturn("https://minio.example.com/skipa/reports/1/report.html?X-Amz-Signature=abc");
        when(reviewRepository.findFirstByPatentIdAndReportIdAndStatusOrderByIdDesc(10L, 1L, ReviewStatus.SUBMITTED))
                .thenReturn(Optional.of(review));

        ReportDetailResponse response = reportService.get(null, 10L, 1L);

        verify(businessPatentAccessValidator).validate(null, 10L);
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.patentId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("REPORT_COMPLETED");
        assertThat(response.totalScore()).isEqualByComparingTo("82.50");
        assertThat(response.valueGrade()).isEqualTo("A");
        assertThat(response.url()).isEqualTo("https://minio.example.com/skipa/reports/1/report.html?X-Amz-Signature=abc");
        assertThat(response.opinion()).isEqualTo("MAINTAIN");
        assertThat(response.comment()).isEqualTo("유지 의견");
        assertThat(response.submittedAt()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
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
        report.completeReport("reports/1/report.json", new BigDecimal("82.50"), "A", null);
        Review review = submittedReview(20L, report, BusinessOpinion.ABANDON, "포기 의견");
        when(reportRepository.findFirstByPatentIdOrderByIdDesc(10L)).thenReturn(Optional.of(report));
        when(reportStorageService.generatePresignedUrl("reports/1/report.json"))
                .thenReturn("https://minio.example.com/reports/1/report.json?signature=abc");
        when(reviewRepository.findFirstByPatentIdAndReportIdAndStatusOrderByIdDesc(10L, 1L, ReviewStatus.SUBMITTED))
                .thenReturn(Optional.of(review));

        ReportDetailResponse response = reportService.getLatest(null, 10L);

        assertThat(response.status()).isEqualTo("REPORT_COMPLETED");
        assertThat(response.totalScore()).isEqualByComparingTo("82.50");
        assertThat(response.valueGrade()).isEqualTo("A");
        assertThat(response.url()).isEqualTo("https://minio.example.com/reports/1/report.json?signature=abc");
        assertThat(response.opinion()).isEqualTo("ABANDON");
        assertThat(response.comment()).isEqualTo("포기 의견");
        assertThat(response.submittedAt()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
    }

    @Test
    void getLatestRejectsMissingReport() {
        when(reportRepository.findFirstByPatentIdOrderByIdDesc(10L)).thenReturn(Optional.empty());

        assertReportError(() -> reportService.getLatest(null, 10L), ErrorCode.REPORT_NOT_FOUND);
    }

    @Test
    void getHistoryReturnsCompletedReportsExceptLatestWithReviewDecision() {
        Report latestReport = completedReport(3L, 10L, "A", "82.50", "2026-03-01T00:00:00Z");
        Report oldReport = completedReport(2L, 10L, "S", "91.00", "2026-02-01T00:00:00Z");
        Report oldestReport = completedReport(1L, 10L, "B", "75.00", "2026-01-01T00:00:00Z");
        Review oldReview = submittedReview(20L, oldReport, BusinessOpinion.MAINTAIN, "유지 의견");
        Review oldestReview = submittedReview(10L, oldestReport, BusinessOpinion.ABANDON, "포기 의견");
        when(reportRepository.findByPatentIdAndStatusInOrderByIdDesc(
                10L,
                List.of(ReportStatus.REPORT_COMPLETED, ReportStatus.EMBEDDING_COMPLETED)
        ))
                .thenReturn(List.of(latestReport, oldReport, oldestReport));
        when(reviewRepository.findByPatentIdAndReportIdInAndStatus(
                10L,
                List.of(2L, 1L),
                ReviewStatus.SUBMITTED
        )).thenReturn(List.of(oldestReview, oldReview));

        ReportHistoryResponse response = reportService.getHistory(null, 10L);

        verify(businessPatentAccessValidator).validate(null, 10L);
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0))
                .extracting("id", "patentId", "totalScore", "valueGrade", "evaluatedAt", "opinion", "comment")
                .containsExactly(
                        2L,
                        10L,
                        new BigDecimal("91.00"),
                        "S",
                        Instant.parse("2026-02-01T00:00:00Z"),
                        "MAINTAIN",
                        "유지 의견"
                );
        assertThat(response.items().get(1))
                .extracting("id", "patentId", "totalScore", "valueGrade", "evaluatedAt", "opinion", "comment")
                .containsExactly(
                        1L,
                        10L,
                        new BigDecimal("75.00"),
                        "B",
                        Instant.parse("2026-01-01T00:00:00Z"),
                        "ABANDON",
                        "포기 의견"
                );
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

    private Report completedReport(Long reportId, Long patentId, String grade, String score, String evaluatedAt) {
        Report report = report(reportId, patentId);
        report.completeReport("reports/%d/report.html".formatted(reportId), new BigDecimal(score), grade, Instant.parse(evaluatedAt));
        return report;
    }

    private Review submittedReview(Long reviewId, Report report, BusinessOpinion opinion, String comment) {
        Department department = Department.builder().name("사업부").build();
        ReflectionTestUtils.setField(department, "id", 1L);
        ReviewCycle reviewCycle = ReviewCycle.builder()
                .year(2026)
                .quarter(1)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .build();
        ReflectionTestUtils.setField(reviewCycle, "id", 1L);
        Review review = Review.builder()
                .patent(report.getPatent())
                .department(department)
                .reviewCycle(reviewCycle)
                .report(report)
                .status(ReviewStatus.SUBMITTED)
                .opinion(opinion)
                .comment(comment)
                .submittedAt(Instant.parse("2026-02-01T00:00:00Z"))
                .build();
        ReflectionTestUtils.setField(review, "id", reviewId);
        return review;
    }

    private void assertReportError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ReportException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
