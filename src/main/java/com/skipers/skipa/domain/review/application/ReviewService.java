package com.skipers.skipa.domain.review.application;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.domain.BusinessOpinion;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import com.skipers.skipa.domain.review.dto.request.BulkReviewCreateRequest;
import com.skipers.skipa.domain.review.dto.response.BulkReviewCreateResponse;
import com.skipers.skipa.domain.review.dto.response.ReviewConfirmResponse;
import com.skipers.skipa.domain.review.dto.response.ReviewResponse;
import com.skipers.skipa.domain.review.dto.response.ReviewStatsResponse;
import com.skipers.skipa.domain.review.exception.ReviewException;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewCycleRepository reviewCycleRepository;
    private final PatentRepository patentRepository;
    private final ReportRepository reportRepository;

    @Transactional
    public ReviewResponse create(Long patentId) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));
        Department department = validateDepartment(patent);
        ReviewCycle reviewCycle = getActiveReviewCycle();

        if (reviewRepository.existsByReviewCycleIdAndPatentIdAndDepartmentId(
                reviewCycle.getId(),
                patentId,
                department.getId()
        )) {
            throw new ReviewException(ErrorCode.DUPLICATE_REVIEW_REQUEST);
        }

        Review review = reviewRepository.save(createReview(
                patent,
                department,
                reviewCycle,
                findLatestReport(patent.getId())
        ));

        return ReviewResponse.from(review);
    }

    @Transactional
    public BulkReviewCreateResponse createBulk(BulkReviewCreateRequest request) {
        ReviewCycle reviewCycle = getActiveReviewCycle();
        List<Long> patentIds = new ArrayList<>(new LinkedHashSet<>(request.patentIds()));
        Map<Long, Patent> patentsById = new HashMap<>();
        patentRepository.findAllById(patentIds).forEach(patent -> patentsById.put(patent.getId(), patent));

        Set<String> existingReviewKeys = new HashSet<>();
        if (!patentsById.isEmpty()) {
            reviewRepository.findAllByReviewCycleIdAndPatentIdIn(reviewCycle.getId(), patentsById.keySet())
                    .forEach(review -> existingReviewKeys.add(reviewKey(review.getPatent().getId(), review.getDepartment().getId())));
        }

        List<Review> reviews = new ArrayList<>();
        List<BulkReviewCreateResponse.Item> items = new ArrayList<>();
        for (Long patentId : patentIds) {
            Patent patent = patentsById.get(patentId);
            if (patent == null) {
                items.add(BulkReviewCreateResponse.Item.skipped(patentId, ErrorCode.PATENT_NOT_FOUND.getCode()));
                continue;
            }

            Department department = patent.getCurrentDepartment();
            if (department == null) {
                items.add(BulkReviewCreateResponse.Item.skipped(
                        patentId,
                        ErrorCode.PATENT_DEPARTMENT_NOT_ASSIGNED.getCode()
                ));
                continue;
            }
            if (department.isInactive()) {
                items.add(BulkReviewCreateResponse.Item.skipped(patentId, ErrorCode.DEPARTMENT_INACTIVE.getCode()));
                continue;
            }
            if (existingReviewKeys.contains(reviewKey(patentId, department.getId()))) {
                items.add(BulkReviewCreateResponse.Item.skipped(patentId, ErrorCode.DUPLICATE_REVIEW_REQUEST.getCode()));
                continue;
            }

            reviews.add(createReview(
                    patent,
                    department,
                    reviewCycle,
                    findLatestReport(patentId)
            ));
            items.add(BulkReviewCreateResponse.Item.created(patentId));
        }

        if (!reviews.isEmpty()) {
            reviewRepository.saveAll(reviews);
        }
        return new BulkReviewCreateResponse(
                reviewCycle.getId(),
                reviews.size(),
                items.size() - reviews.size(),
                items
        );
    }

    public Page<ReviewResponse> getAll(
            String status,
            Long departmentId,
            Long patentId,
            Boolean checked,
            Pageable pageable
    ) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        return reviewRepository.findAllByFilters(parseStatus(status), departmentId, patentId, checked, sortedPageable)
                .map(ReviewResponse::from);
    }

    public ReviewResponse get(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));

        return ReviewResponse.from(review);
    }

    public ReviewStatsResponse getStats() {
        ReviewCycle reviewCycle = getActiveReviewCycle();
        LocalDate today = LocalDate.now();
        List<Review> reviews = reviewRepository.findAllByReviewCycleId(reviewCycle.getId());
        long unassigned = patentRepository.countByCurrentDepartmentIsNull();

        long requested = 0;
        long overdue = 0;
        long done = 0;
        long unread = 0;
        long maintain = 0;
        long abandon = 0;
        Map<Long, DepartmentOpinionStats> byDepartment = new HashMap<>();
        Map<String, OpinionStats> byTechField = new HashMap<>();

        for (Review review : reviews) {
            if (review.getStatus() == ReviewStatus.PENDING) {
                if (review.getDueDate().isBefore(today)) {
                    overdue++;
                } else {
                    requested++;
                }
                continue;
            }

            if (review.getStatus() != ReviewStatus.SUBMITTED) {
                continue;
            }

            done++;
            if (!review.isChecked()) {
                unread++;
            }

            if (review.getOpinion() == BusinessOpinion.MAINTAIN) {
                maintain++;
                departmentOpinionStats(byDepartment, review).maintain++;
                byTechField.computeIfAbsent(techFieldName(review), ignored -> new OpinionStats()).maintain++;
            } else if (review.getOpinion() == BusinessOpinion.ABANDON) {
                abandon++;
                departmentOpinionStats(byDepartment, review).abandon++;
                byTechField.computeIfAbsent(techFieldName(review), ignored -> new OpinionStats()).abandon++;
            }
        }

        long total = reviews.size() + unassigned;
        return new ReviewStatsResponse(
                reviewCycle.getId(),
                reviewCycle.getName(),
                total,
                unassigned,
                requested,
                overdue,
                done,
                unread,
                maintain,
                abandon,
                progressRate(done, total),
                departmentStats(byDepartment),
                techFieldStats(byTechField)
        );
    }

    @Transactional
    public ReviewConfirmResponse confirm(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));
        if (review.getStatus() != ReviewStatus.SUBMITTED) {
            throw new ReviewException(ErrorCode.INVALID_REVIEW_STATUS);
        }

        review.confirm();
        return ReviewConfirmResponse.from(review);
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

    private Department validateDepartment(Patent patent) {
        Department department = patent.getCurrentDepartment();
        if (department == null) {
            throw new ReviewException(ErrorCode.PATENT_DEPARTMENT_NOT_ASSIGNED);
        }
        if (department.isInactive()) {
            throw new ReviewException(ErrorCode.DEPARTMENT_INACTIVE);
        }
        return department;
    }

    private ReviewCycle getActiveReviewCycle() {
        LocalDate today = LocalDate.now();
        return reviewCycleRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(today, today)
                .orElseThrow(() -> new ReviewException(ErrorCode.ACTIVE_REVIEW_CYCLE_NOT_FOUND));
    }

    private Review createReview(Patent patent, Department department, ReviewCycle reviewCycle, Report report) {
        return Review.builder()
                .patent(patent)
                .department(department)
                .reviewCycle(reviewCycle)
                .report(report)
                .build();
    }

    private Report findLatestReport(Long patentId) {
        return reportRepository.findFirstByPatentIdOrderByIdDesc(patentId).orElse(null);
    }

    private String reviewKey(Long patentId, Long departmentId) {
        return patentId + ":" + departmentId;
    }

    private DepartmentOpinionStats departmentOpinionStats(
            Map<Long, DepartmentOpinionStats> byDepartment,
            Review review
    ) {
        return byDepartment.computeIfAbsent(
                review.getDepartment().getId(),
                departmentId -> new DepartmentOpinionStats(departmentId, review.getDepartment().getName())
        );
    }

    private double progressRate(long done, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round(done * 1000.0 / total) / 10.0;
    }

    private List<ReviewStatsResponse.DepartmentStats> departmentStats(
            Map<Long, DepartmentOpinionStats> statsByDepartment
    ) {
        return statsByDepartment.values().stream()
                .sorted(Comparator.comparing(DepartmentOpinionStats::departmentName))
                .map(stats -> new ReviewStatsResponse.DepartmentStats(
                        stats.departmentId(),
                        stats.departmentName(),
                        stats.maintain,
                        stats.abandon
                ))
                .toList();
    }

    private List<ReviewStatsResponse.TechFieldStats> techFieldStats(Map<String, OpinionStats> statsByTechField) {
        return statsByTechField.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ReviewStatsResponse.TechFieldStats(
                        entry.getKey(),
                        entry.getValue().maintain,
                        entry.getValue().abandon
                ))
                .toList();
    }

    private String techFieldName(Review review) {
        String techField = review.getPatent().getTechField();
        return techField == null || techField.isBlank() ? "미분류" : techField;
    }

    private static class OpinionStats {
        long maintain;
        long abandon;
    }

    private static class DepartmentOpinionStats extends OpinionStats {
        private final Long departmentId;
        private final String departmentName;

        private DepartmentOpinionStats(Long departmentId, String departmentName) {
            this.departmentId = departmentId;
            this.departmentName = departmentName;
        }

        private Long departmentId() {
            return departmentId;
        }

        private String departmentName() {
            return departmentName;
        }
    }
}
