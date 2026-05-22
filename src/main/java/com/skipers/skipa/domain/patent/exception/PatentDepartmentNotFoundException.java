/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: PatentDepartmentNotFoundException 예외 클래스 뼈대 정의.
 * 역할: 도메인/비즈니스 예외를 표현.
 */
package com.skipers.skipa.domain.patent.exception;

public class PatentDepartmentNotFoundException extends RuntimeException {
    public PatentDepartmentNotFoundException() {
    }

    public PatentDepartmentNotFoundException(String message) {
        super(message);
    }
}
