/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: 서비스 표준 에러 코드 기반의 비즈니스 예외를 제공한다.
 * 역할: 도메인/서비스 예외를 `ErrorCode`로 묶어 전역 예외 처리로 전달한다.
 *
 * 사용법:
 * - `throw new BusinessException(ErrorCode.INVALID_REQUEST, "메시지")`
 * - 도메인 예외가 필요하면 `BusinessException`을 상속해서 사용한다.
 */
package com.skipers.skipa.global.exception;

import lombok.Getter;

/**
 * 표준 에러 코드 기반 비즈니스 예외.
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 표준 에러 코드. */
    private final ErrorCode errorCode;

    /** 기본 메시지로 예외를 생성한다. */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 커스텀 메시지로 예외를 생성한다. */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
