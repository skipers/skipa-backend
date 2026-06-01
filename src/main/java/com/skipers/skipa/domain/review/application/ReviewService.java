package com.skipers.skipa.domain.review.application;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.department.exception.DepartmentException;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.dto.request.ReviewCreateRequest;
import com.skipers.skipa.domain.review.dto.response.ReviewResponse;
import com.skipers.skipa.domain.review.exception.ReviewException;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
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
}
