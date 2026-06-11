package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dto.response.BusinessReviewDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.BusinessReviewHistoryResponse;
import com.skipers.skipa.domain.patent.dto.response.BusinessReviewResponse;
import com.skipers.skipa.domain.patent.dto.response.BusinessReviewSummaryResponse;
import com.skipers.skipa.domain.portfolio.application.PortfolioInsightCacheInvalidator;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.domain.ReportStatus;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.BusinessOpinion;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import com.skipers.skipa.domain.review.dto.request.ReviewSubmitRequest;
import com.skipers.skipa.domain.review.exception.ReviewException;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewCycleRepository reviewCycleRepository;
    private final ReportRepository reportRepository;
    private final PatentService patentService;
    private final BusinessPatentAccessValidator businessPatentAccessValidator;
    private final ApprovedPatentValidator approvedPatentValidator;
    private final PortfolioInsightCacheInvalidator portfolioInsightCacheInvalidator;

    public BusinessReviewSummaryResponse getSummary(User user) {
        Long departmentId = getDepartmentId(user);
        ReviewCycle reviewCycle = getActiveReviewCycle();
        List<Review> reviews = currentDepartmentReviews(reviewCycle.getId(), departmentId);
        long submitted = reviews.stream()
                .filter(review -> review.getStatus() == ReviewStatus.SUBMITTED)
                .count();

        return new BusinessReviewSummaryResponse(
                BusinessReviewSummaryResponse.ReviewCycleInfo.from(reviewCycle),
                new BusinessReviewSummaryResponse.Kpi(submitted, reviews.size() - submitted)
        );
    }

    public Page<BusinessReviewResponse> getAll(
            User user,
            String status,
            String opinion,
            LocalDate submittedFrom,
            LocalDate submittedTo,
            String sort,
            Pageable pageable
    ) {
        Long departmentId = getDepartmentId(user);
        ReviewStatus parsedStatus = parseStatus(status);
        BusinessOpinion parsedOpinion = parseOpinion(opinion);
        ReviewCycle reviewCycle = getActiveReviewCycle();
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                PatentSortOption.parse(sort).reviewSort()
        );

        Page<Review> reviews = reviewRepository.findLatestBusinessReviewsByReviewCycleIdAndDepartmentId(
                        reviewCycle.getId(),
                        departmentId,
                        parsedStatus,
                        parsedOpinion,
                        submittedFrom != null,
                        startOfDay(submittedFrom),
                        submittedTo != null,
                        nextDayStart(submittedTo),
                        sortedPageable
                );
        List<Review> reviewItems = reviews.getContent();
        Map<Long, Report> reportsByPatentId = latestCompletedReportsByPatentId(reviewItems);
        List<BusinessReviewResponse> responses = reviewItems.stream().map(review -> {
            Report report = reportsByPatentId.get(review.getPatent().getId());
            return BusinessReviewResponse.from(
                    review,
                    report == null ? null : report.getTotalScore(),
                    report == null ? null : report.getValueGrade()
            );
        }).toList();
        return new PageImpl<>(responses, sortedPageable, reviews.getTotalElements());
    }

    public Page<BusinessReviewHistoryResponse> getHistory(
            User user,
            Integer year,
            Integer quarter,
            String opinion,
            Pageable pageable
    ) {
        Long departmentId = getDepartmentId(user);
        validateHistoryFilter(year, quarter);
        BusinessOpinion parsedOpinion = parseOpinion(opinion);

        Page<Review> reviews = reviewRepository.findSubmittedBusinessReviewHistory(
                departmentId,
                LocalDate.now(),
                year,
                quarter,
                parsedOpinion,
                pageable
        );
        List<Review> approvedReviews = reviews.getContent().stream()
                .filter(this::isApprovedPatent)
                .toList();
        Map<Long, Report> reportsByPatentId = latestCompletedReportsByPatentId(approvedReviews);
        List<BusinessReviewHistoryResponse> responses = approvedReviews.stream().map(review -> {
            Report report = reportsByPatentId.get(review.getPatent().getId());
            return BusinessReviewHistoryResponse.from(
                    review,
                    report == null ? null : report.getTotalScore(),
                    report == null ? null : report.getValueGrade()
            );
        }).toList();
        return new PageImpl<>(responses, pageable, responses.size());
    }

    public BusinessReviewDetailResponse get(User user, Long patentId) {
        Review review = getOwnedReview(user, patentId);

        return BusinessReviewDetailResponse.of(
                patentService.get(patentId),
                review
        );
    }

    @Transactional
    public BusinessReviewResponse submit(
            User user,
            Long patentId,
            ReviewSubmitRequest request
    ) {
        Review review = getOwnedReview(user, patentId);
        if (review.getStatus() != ReviewStatus.PENDING && review.getStatus() != ReviewStatus.OVERDUE) {
            throw new ReviewException(ErrorCode.OPINION_ALREADY_SUBMITTED);
        }

        BusinessOpinion opinion;
        try {
            opinion = BusinessOpinion.valueOf(request.opinion());
        } catch (IllegalArgumentException e) {
            throw new ReviewException(ErrorCode.INVALID_REQUEST);
        }

        review.submit(opinion, request.comment(), Instant.now());

        portfolioInsightCacheInvalidator.evict();
        return BusinessReviewResponse.from(review);
    }

    private Review getOwnedReview(User user, Long patentId) {
        Long departmentId = getDepartmentId(user);
        ReviewCycle reviewCycle = getActiveReviewCycle();
        businessPatentAccessValidator.validate(user, patentId);
        approvedPatentValidator.getApprovedPatent(patentId);

        return reviewRepository.findFirstByReviewCycleIdAndPatentIdAndDepartmentIdAndStatusInOrderByIdDesc(
                        reviewCycle.getId(),
                        patentId,
                        departmentId,
                        List.of(ReviewStatus.PENDING, ReviewStatus.OVERDUE, ReviewStatus.SUBMITTED)
                )
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private ReviewCycle getActiveReviewCycle() {
        LocalDate today = LocalDate.now();
        return reviewCycleRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(today, today)
                .orElseThrow(() -> new ReviewException(ErrorCode.ACTIVE_REVIEW_CYCLE_NOT_FOUND));
    }

    private List<Review> currentDepartmentReviews(Long reviewCycleId, Long departmentId) {
        return reviewRepository.findAllByReviewCycleId(reviewCycleId).stream()
                .filter(review -> review.getDepartment().getId().equals(departmentId))
                .filter(review -> review.getPatent().getCurrentDepartment() != null)
                .filter(review -> review.getPatent().getCurrentDepartment().getId().equals(departmentId))
                .filter(review -> review.getStatus() != ReviewStatus.SCHEDULED)
                .filter(this::isApprovedPatent)
                .toList();
    }

    private boolean isApprovedPatent(Review review) {
        try {
            approvedPatentValidator.validateApproved(review.getPatent());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private Map<Long, Report> latestCompletedReportsByPatentId(List<Review> reviews) {
        Map<Long, Report> reportsByPatentId = new HashMap<>();
        if (reviews.isEmpty()) {
            return reportsByPatentId;
        }

        reportRepository.findAllByStatus(ReportStatus.COMPLETED).stream()
                .filter(report -> reviews.stream()
                        .anyMatch(review -> review.getPatent().getId().equals(report.getPatent().getId())))
                .sorted(Comparator.comparing(Report::getId).reversed())
                .forEach(report -> reportsByPatentId.putIfAbsent(report.getPatent().getId(), report));
        return reportsByPatentId;
    }

    private ReviewStatus parseStatus(String status) {
        if (status == null) {
            return null;
        }

        try {
            return ReviewStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new ReviewException(ErrorCode.INVALID_REQUEST);
        }
    }

    private BusinessOpinion parseOpinion(String opinion) {
        if (opinion == null) {
            return null;
        }

        try {
            return BusinessOpinion.valueOf(opinion);
        } catch (IllegalArgumentException e) {
            throw new ReviewException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateHistoryFilter(Integer year, Integer quarter) {
        if (year == null && quarter != null) {
            throw new ReviewException(ErrorCode.INVALID_REQUEST);
        }
        if (quarter != null && (quarter < 1 || quarter > 4)) {
            throw new ReviewException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Instant startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Instant nextDayStart(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Long getDepartmentId(User user) {
        if (user.getDepartment() == null) {
            throw new ReviewException(ErrorCode.FORBIDDEN);
        }

        return user.getDepartment().getId();
    }
}
