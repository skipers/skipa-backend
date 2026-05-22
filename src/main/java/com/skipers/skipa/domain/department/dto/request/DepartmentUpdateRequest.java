/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: 부서명 변경 요청 시 전달되는 입력 DTO를 정의한다.
 * 역할: `PUT/PATCH /departments/{departmentId}` 요청 바디를 역직렬화해 서비스 계층으로 전달한다.
 */
package com.skipers.skipa.domain.department.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부서 수정 요청 DTO.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepartmentUpdateRequest {

    /** 변경할 부서명. */
    private String name;

    /** 부서 수정 요청을 구성합니다. */
    public DepartmentUpdateRequest(String name) {
        this.name = name;
    }
}
