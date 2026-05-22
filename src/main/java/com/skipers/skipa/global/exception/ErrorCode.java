/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: 서비스에서 사용하는 표준 에러 코드를 정의한다.
 * 역할: 예외 → HTTP 상태/코드/메시지 매핑을 일관되게 관리한다.
 *
 * 사용법:
 * - `BusinessException` 생성 시 `ErrorCode`를 지정한다.
 * - `GlobalExceptionHandler`가 `ErrorCode`를 기준으로 응답을 생성한다.
 */
package com.skipers.skipa.global.exception;

import lombok.Getter;

/**
 * 표준 에러 코드.
 */
@Getter
public enum ErrorCode {

    /** 요청 값이 잘못된 경우. */
    INVALID_REQUEST(400, "COMMON_400", "잘못된 요청입니다."),

    /** 부서를 찾을 수 없는 경우. */
    DEPARTMENT_NOT_FOUND(404, "DEPARTMENT_404", "부서를 찾을 수 없습니다."),

    /** 동일한 부서명이 이미 존재하는 경우. */
    DUPLICATE_DEPARTMENT_NAME(409, "DEPARTMENT_409", "이미 존재하는 부서명입니다."),

    /** 서버 내부 오류. */
    INTERNAL_ERROR(500, "COMMON_500", "서버 오류가 발생했습니다.");

    /** HTTP 상태 코드. */
    private final int status;

    /** 에러 코드 문자열. */
    private final String code;

    /** 기본 메시지. */
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
