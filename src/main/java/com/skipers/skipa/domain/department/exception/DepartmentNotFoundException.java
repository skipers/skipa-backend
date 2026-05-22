/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: 존재하지 않는 부서(사업부)를 조회/수정/삭제하려는 상황을 표현한다.
 * 역할: Service 계층에서 조회 실패를 명확한 예외로 전달한다.
 *
 * 사용법:
 * - `DepartmentRepository` 조회 결과가 없을 때 throw 한다.
 * - ID/부서명에 맞는 정적 팩토리 메서드를 사용한다.
 */
package com.skipers.skipa.domain.department.exception;

import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;

/**
 * 존재하지 않는 부서를 조회했을 때 발생하는 예외.
 */
public class DepartmentNotFoundException extends BusinessException {

    /** 기본 생성자(공통 메시지). */
    public DepartmentNotFoundException() {
        super(ErrorCode.DEPARTMENT_NOT_FOUND, "부서를 찾을 수 없습니다.");
    }

    /** 커스텀 메시지로 예외를 생성한다. */
    public DepartmentNotFoundException(String message) {
        super(ErrorCode.DEPARTMENT_NOT_FOUND, message);
    }

    /** 부서 ID 기준 조회 실패 예외를 생성한다. */
    public static DepartmentNotFoundException withId(Long departmentId) {
        return new DepartmentNotFoundException("부서를 찾을 수 없습니다. departmentId=" + departmentId);
    }

    /** 부서명 기준 조회 실패 예외를 생성한다. */
    public static DepartmentNotFoundException withName(String name) {
        return new DepartmentNotFoundException("부서를 찾을 수 없습니다. name=" + name);
    }
}
