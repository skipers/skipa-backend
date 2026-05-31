package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.PatentDepartment;

import java.time.Instant;

public record PatentDepartmentResponse(
        Long id,
        Long patentId,
        Long departmentId,
        String departmentName, // 프론트 편의용(부서명 표시)
        Instant assignedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static PatentDepartmentResponse from(PatentDepartment patentDepartment) {
        return new PatentDepartmentResponse(
                patentDepartment.getId(),
                patentDepartment.getPatent().getId(),
                patentDepartment.getDepartment().getId(),
                patentDepartment.getDepartment().getName(),
                patentDepartment.getAssignedAt(),
                patentDepartment.getCreatedAt(),
                patentDepartment.getUpdatedAt()
        );
    }
}
