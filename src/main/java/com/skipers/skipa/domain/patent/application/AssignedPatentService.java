package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.patent.dto.response.AssignedPatentDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.AssignedPatentResponse;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignedPatentService {

    private final ReviewRepository reviewRepository;
    private final PatentService patentService;

    public Page<AssignedPatentResponse> getAll(User user, Pageable pageable) {
        Long departmentId = getDepartmentId(user);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        return reviewRepository.findByDepartmentId(departmentId, sortedPageable)
                .map(AssignedPatentResponse::from);
    }

    public AssignedPatentDetailResponse get(User user, Long patentId) {
        Review review = getOwnedReview(user, patentId);

        return AssignedPatentDetailResponse.of(
                patentService.get(patentId),
                review
        );
    }

    @Transactional
    public AssignedPatentResponse submit(
            User user,
            Long patentId,
            ReviewSubmitRequest request
    ) {
        Review review = getOwnedReview(user, patentId);

        if (review.getStatus() == ReviewStatus.제출완료) {
            throw new ReviewException(ErrorCode.OPINION_ALREADY_SUBMITTED);
        }

        BusinessOpinion opinion;
        try {
            opinion = BusinessOpinion.valueOf(request.opinion());
        } catch (IllegalArgumentException e) {
            throw new ReviewException(ErrorCode.INVALID_REQUEST);
        }

        review.submit(opinion, request.comment(), Instant.now());

        return AssignedPatentResponse.from(review);
    }

    private Review getOwnedReview(User user, Long patentId) {
        Long departmentId = getDepartmentId(user);

        return reviewRepository.findByPatentIdAndDepartmentId(patentId, departmentId)
                .orElseThrow(() -> reviewRepository.existsByPatentId(patentId)
                        ? new ReviewException(ErrorCode.FORBIDDEN)
                        : new ReviewException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private Long getDepartmentId(User user) {
        if (user.getDepartment() == null) {
            throw new ReviewException(ErrorCode.FORBIDDEN);
        }

        return user.getDepartment().getId();
    }
}
