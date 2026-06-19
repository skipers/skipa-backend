package com.skipers.skipa.domain.review.dto.response;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.review.domain.Review;

import java.time.Instant;
import java.time.LocalDate;

public record ReviewResponse(
        Long id,
        Long patentId,
        String title,
        String applicationNumber,
        String techField,
        String businessField,
        Long reportId,
        Long departmentId,
        String departmentName,
        Long reviewCycleId,
        Integer reviewCycleYear,
        Integer reviewCycleQuarter,
        String opinion,
        String comment,
        String status,
        boolean checked,
        Instant submittedAt,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt
) {

    public static ReviewResponse from(Review review) {
        Department displayDepartment = review.getPatent().getCurrentDepartment() != null
                ? review.getPatent().getCurrentDepartment()
                : review.getDepartment();
        return new ReviewResponse(
                review.getId(),
                review.getPatent().getId(),
                review.getPatent().getTitle(),
                review.getPatent().getApplicationNumber(),
                review.getPatent().getTechField(),
                review.getPatent().getBusinessField(),
                review.getReport() == null ? null : review.getReport().getId(),
                displayDepartment == null ? null : displayDepartment.getId(),
                displayDepartment == null ? null : displayDepartment.getName(),
                review.getReviewCycle().getId(),
                review.getReviewCycle().getYear(),
                review.getReviewCycle().getQuarter(),
                review.getOpinion() != null ? review.getOpinion().name() : null,
                review.getComment(),
                review.getStatus().name(),
                review.isChecked(),
                review.getSubmittedAt(),
                review.getDueDate(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
