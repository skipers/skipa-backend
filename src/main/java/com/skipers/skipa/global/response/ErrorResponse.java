/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: API 실패 응답에서 반환할 에러 정보를 정의한다.
 * 역할: 에러 코드/메시지를 표준 형태로 제공한다.
 *
 * 사용법:
 * - 예외 핸들러에서 `ErrorResponse.of(code, message)` 형태로 생성한다.
 */
package com.skipers.skipa.global.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공통 에러 응답.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorResponse {

    /** 에러 코드(서비스 내부 규격). */
    private String code;

    /** 사용자/클라이언트에 전달할 에러 메시지. */
    private String message;

    private ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /** 에러 응답을 생성한다. */
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message);
    }
}
