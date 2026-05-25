/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: PatentNotFoundException 예외 클래스 뼈대 정의.
 * 역할: 도메인/비즈니스 예외를 표현.
 */
package com.skipers.skipa.domain.patent.exception;

public class PatentNotFoundException extends RuntimeException {
    public PatentNotFoundException() {
    }

    public PatentNotFoundException(String message) {
        super(message);
    }
}
