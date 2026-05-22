/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: 모든 API 응답의 최상위(JSON) 형태를 통일한다.
 * 역할: 성공/실패 여부와 데이터/에러를 일관된 구조로 전달한다.
 *
 * 사용법:
 * - 성공 응답: `ApiResponse.success(data)`
 * - 실패 응답: `ApiResponse.failure(errorResponse)`
 */
package com.skipers.skipa.global.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공통 API 응답 래퍼.
 *
 * `success=true`면 `data`가 채워지고, `success=false`면 `error`가 채워진다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {

    /** API 처리 성공 여부. */
    private boolean success;

    /** 성공 시 응답 데이터. */
    private T data;

    /** 실패 시 에러 정보. */
    private ErrorResponse error;

    private ApiResponse(boolean success, T data, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    /** 성공 응답을 생성한다. */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** 실패 응답을 생성한다. */
    public static <T> ApiResponse<T> failure(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }
}
