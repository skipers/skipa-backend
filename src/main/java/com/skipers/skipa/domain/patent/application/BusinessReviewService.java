package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dto.response.BusinessReviewDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.BusinessReviewResponse;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.BusinessOpinion;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import com.skipers.skipa.domain.review.dto.request.ReviewSubmitRequest;
import com.skipers.skipa.domain.review.exception.ReviewException;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessReviewService {

    private final ReviewRepository reviewRepository;
    private final PatentService patentService;
    private final BusinessPatentAccessValidator businessPatentAccessValidator;

    public Page<BusinessReviewResponse> getAll(
            User user,
            String status,
            String opinion,
            LocalDate submittedFrom,
            LocalDate submittedTo,
            Pageable pageable
    ) {
        Long departmentId = getDepartmentId(user);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        return reviewRepository.findLatestBusinessReviewsByDepartmentId(
                        departmentId,
                        parseStatus(status),
                        parseOpinion(opinion),
                        startOfDay(submittedFrom),
                        nextDayStart(submittedTo),
                        sortedPageable
                )
                .map(BusinessReviewResponse::from);
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
        if (review.getStatus() != ReviewStatus.PENDING) {
            throw new ReviewException(ErrorCode.OPINION_ALREADY_SUBMITTED);
        }
        if (review.getDueDate().isBefore(LocalDate.now())) {
            throw new ReviewException(ErrorCode.REVIEW_DEADLINE_EXPIRED);
        }

        BusinessOpinion opinion;
        try {
            opinion = BusinessOpinion.valueOf(request.opinion());
        } catch (IllegalArgumentException e) {
            throw new ReviewException(ErrorCode.INVALID_REQUEST);
        }

        review.submit(opinion, request.comment(), Instant.now());

        return BusinessReviewResponse.from(review);
    }

    private Review getOwnedReview(User user, Long patentId) {
        Long departmentId = getDepartmentId(user);
        businessPatentAccessValidator.validate(user, patentId);

        return reviewRepository.findFirstByPatentIdAndDepartmentIdAndStatusInOrderByIdDesc(
                        patentId,
                        departmentId,
                        List.of(ReviewStatus.PENDING, ReviewStatus.SUBMITTED)
                )
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));
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
