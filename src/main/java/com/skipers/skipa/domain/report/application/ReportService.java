package com.skipers.skipa.domain.report.application;

import com.skipers.skipa.domain.patent.application.ApprovedPatentValidator;
import com.skipers.skipa.domain.patent.application.BusinessPatentAccessValidator;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.portfolio.application.PortfolioInsightCacheInvalidator;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.domain.ReportStatus;
import com.skipers.skipa.domain.report.dto.response.ReportCreateResponse;
import com.skipers.skipa.domain.report.dto.response.ReportDetailResponse;
import com.skipers.skipa.domain.report.dto.response.ReportHistoryResponse;
import com.skipers.skipa.domain.report.dto.response.ReportResponse;
import com.skipers.skipa.domain.report.dto.response.ReportStatusResponse;
import com.skipers.skipa.domain.report.exception.ReportException;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final BusinessPatentAccessValidator businessPatentAccessValidator;
    private final ApprovedPatentValidator approvedPatentValidator;
    private final ReportGenerationPublisher reportGenerationPublisher;
    private final ReportStorageService reportStorageService;
    private final ReviewRepository reviewRepository;
    private final PortfolioInsightCacheInvalidator portfolioInsightCacheInvalidator;

    private static final List<ReportStatus> REPORT_GENERATED_STATUSES = List.of(
            ReportStatus.REPORT_COMPLETED,
            ReportStatus.EMBEDDING_COMPLETED
    );

    @Transactional
    public ReportCreateResponse create(Long patentId) {
        Patent patent = approvedPatentValidator.getApprovedPatent(patentId);

        Report report = reportRepository.save(Report.builder()
                .patent(patent)
                .status(ReportStatus.GENERATING)
                .build());

        publishReportGenerationMessage(report);

        return ReportCreateResponse.from(report);
    }

    public Page<ReportResponse> getAll(User user, Long patentId, Pageable pageable) {
        businessPatentAccessValidator.validate(user, patentId);
        approvedPatentValidator.getApprovedPatent(patentId);

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        return reportRepository.findByPatentId(patentId, sortedPageable).map(ReportResponse::from);
    }

    public ReportDetailResponse get(User user, Long patentId, Long reportId) {
        businessPatentAccessValidator.validate(user, patentId);
        approvedPatentValidator.getApprovedPatent(patentId);

        Report report = reportRepository.findByIdAndPatentId(reportId, patentId)
                .orElseThrow(() -> new ReportException(ErrorCode.REPORT_NOT_FOUND));

        if (!report.isReportGenerated()) {
            throw new ReportException(ErrorCode.REPORT_NOT_COMPLETED);
        }

        Review review = latestSubmittedReview(patentId, report.getId());
        return ReportDetailResponse.of(report, reportStorageService.generatePresignedUrl(report.getReportKey()), review);
    }

    public ReportDetailResponse getLatest(User user, Long patentId) {
        businessPatentAccessValidator.validate(user, patentId);
        approvedPatentValidator.getApprovedPatent(patentId);

        Report report = reportRepository.findFirstByPatentIdOrderByIdDesc(patentId)
                .orElseThrow(() -> new ReportException(ErrorCode.REPORT_NOT_FOUND));

        String url = report.isReportGenerated() ? reportStorageService.generatePresignedUrl(report.getReportKey()) : null;
        Review review = latestSubmittedReview(patentId, report.getId());
        return ReportDetailResponse.of(report, url, review);
    }

    public ReportHistoryResponse getHistory(User user, Long patentId) {
        businessPatentAccessValidator.validate(user, patentId);
        approvedPatentValidator.getApprovedPatent(patentId);

        List<Report> historyReports = reportRepository
                .findByPatentIdAndStatusInOrderByIdDesc(patentId, REPORT_GENERATED_STATUSES)
                .stream()
                .skip(1)
                .toList();
        if (historyReports.isEmpty()) {
            return new ReportHistoryResponse(List.of());
        }

        Map<Long, Review> latestSubmittedReviewByReportId = latestSubmittedReviewByReportId(patentId, historyReports);
        return new ReportHistoryResponse(historyReports.stream()
                .map(report -> {
                    Review review = latestSubmittedReviewByReportId.get(report.getId());
                    return new ReportHistoryResponse.Item(
                            report.getId(),
                            report.getPatent().getId(),
                            report.getTotalScore(),
                            report.getValueGrade(),
                            report.getEvaluatedAt(),
                            review == null || review.getOpinion() == null ? null : review.getOpinion().name(),
                            review == null ? null : review.getComment()
                    );
                })
                .toList());
    }

    public ReportStatusResponse getStatus(User user, Long patentId, Long reportId) {
        businessPatentAccessValidator.validate(user, patentId);
        approvedPatentValidator.getApprovedPatent(patentId);

        Report report = reportRepository.findByIdAndPatentId(reportId, patentId)
                .orElseThrow(() -> new ReportException(ErrorCode.REPORT_NOT_FOUND));

        return ReportStatusResponse.from(report);
    }

    @Transactional
    public ReportStatusResponse complete(Long reportId, String reportKey, BigDecimal totalScore, String valueGrade) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ErrorCode.REPORT_NOT_FOUND));

        report.completeReport(reportKey, totalScore, valueGrade, Instant.now());

        portfolioInsightCacheInvalidator.evict();
        return ReportStatusResponse.from(report);
    }

    @Transactional
    public ReportStatusResponse completeEmbedding(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ErrorCode.REPORT_NOT_FOUND));

        report.completeEmbedding();

        return ReportStatusResponse.from(report);
    }

    @Transactional
    public ReportStatusResponse fail(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ErrorCode.REPORT_NOT_FOUND));

        report.fail();

        return ReportStatusResponse.from(report);
    }

    private void publishReportGenerationMessage(Report report) {
        try {
            reportGenerationPublisher.publish(report.getId(), report.getPatent().getId());
        } catch (RuntimeException e) {
            throw new ReportException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }

    private Map<Long, Review> latestSubmittedReviewByReportId(Long patentId, List<Report> reports) {
        List<Long> reportIds = reports.stream()
                .map(Report::getId)
                .toList();
        return reviewRepository.findByPatentIdAndReportIdInAndStatus(patentId, reportIds, ReviewStatus.SUBMITTED)
                .stream()
                .sorted(Comparator.comparing(Review::getId).reversed())
                .collect(Collectors.toMap(
                        review -> review.getReport().getId(),
                        review -> review,
                        (existing, ignored) -> existing
                ));
    }

    private Review latestSubmittedReview(Long patentId, Long reportId) {
        return reviewRepository.findFirstByPatentIdAndReportIdAndStatusOrderByIdDesc(
                        patentId,
                        reportId,
                        ReviewStatus.SUBMITTED
                )
                .orElse(null);
    }
}
