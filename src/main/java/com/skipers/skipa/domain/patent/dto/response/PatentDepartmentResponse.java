package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.PatentDepartment;

import java.time.Instant;

public record PatentDepartmentResponse(
        Long id, // 매핑 ID
        Long patentId, // 특허 ID
        Long departmentId, // 부서 ID
        String departmentName, // 부서명(표시용)
        Instant assignedAt, // 배정일시
        Instant createdAt, // 생성일시
        Instant updatedAt // 수정일시
) {

    public static PatentDepartmentResponse from(PatentDepartment mapping) { // 엔티티 → 응답 DTO 변환
        return new PatentDepartmentResponse(
                mapping.getId(),
                mapping.getPatent().getId(),
                mapping.getDepartment().getId(),
                mapping.getDepartment().getName(),
                mapping.getAssignedAt(),
                mapping.getCreatedAt(),
                mapping.getUpdatedAt()
        );
    }
}
