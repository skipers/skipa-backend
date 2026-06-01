package com.skipers.skipa.domain.review.application;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.department.exception.DepartmentException;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import com.skipers.skipa.domain.review.dto.request.ReviewCreateRequest;
import com.skipers.skipa.domain.review.dto.response.ReviewResponse;
import com.skipers.skipa.domain.review.exception.ReviewException;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PatentRepository patentRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public ReviewResponse create(Long patentId, ReviewCreateRequest request) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new DepartmentException(ErrorCode.DEPARTMENT_NOT_FOUND));

        if (reviewRepository.existsByPatentIdAndDepartmentId(patentId, request.departmentId())) {
            throw new ReviewException(ErrorCode.DUPLICATE_REVIEW_REQUEST);
        }

        Review review = reviewRepository.save(Review.builder()
                .patent(patent)
                .department(department)
                .build());

        return ReviewResponse.from(review);
    }

    public Page<ReviewResponse> getAll(String status, Long departmentId, Long patentId, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        return reviewRepository.findAllByFilters(parseStatus(status), departmentId, patentId, sortedPageable)
                .map(ReviewResponse::from);
    }

    public ReviewResponse get(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));

        return ReviewResponse.from(review);
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
}
