/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: 부서 조회/생성/수정 API에서 응답으로 반환할 DTO를 정의한다.
 * 역할: Department 엔티티의 주요 값을 직렬화 가능한 형태로 변환해 반환한다.
 */
package com.skipers.skipa.domain.department.dto.response;

import com.skipers.skipa.domain.department.domain.Department;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부서 응답 DTO.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepartmentResponse {

    /** 부서 ID. */
    private Long id;

    /** 부서명. */
    private String name;

    /** 생성일시. */
    private Instant createdAt;

    /** 수정일시. */
    private Instant updatedAt;

    /** 엔티티 기반으로 응답 DTO를 구성합니다. */
    public DepartmentResponse(Long id, String name, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Department 엔티티를 DepartmentResponse로 변환합니다. */
    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }
}
