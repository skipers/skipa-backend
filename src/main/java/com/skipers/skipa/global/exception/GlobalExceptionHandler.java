/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: 애플리케이션 전역 예외를 공통 응답 형식으로 변환한다.
 * 역할: 예외를 HTTP 상태 코드 + `ApiResponse` 형태로 반환해 클라이언트가 일관되게 처리하도록 한다.
 *
 * 사용법:
 * - Controller에서 예외가 발생하면 이 클래스가 응답 생성까지 처리한다.
 * - 비즈니스 예외는 `BusinessException`과 `ErrorCode`를 통해 표준화한다.
 */
package com.skipers.skipa.global.exception;

import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 표준 비즈니스 예외를 처리한다. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ErrorResponse error = ErrorResponse.of(errorCode.getCode(), exception.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.failure(error));
    }

    /** 요청 값 오류 등을 처리한다. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        ErrorResponse error = ErrorResponse.of(errorCode.getCode(), exception.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.failure(error));
    }

    /** 처리되지 않은 예외를 처리한다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        ErrorResponse error = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(error));
    }
}
