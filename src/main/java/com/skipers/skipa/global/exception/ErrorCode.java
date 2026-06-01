package com.skipers.skipa.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 파라미터가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "요청한 리소스의 상태가 충돌합니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."),

    // Auth
    INVALID_LOGIN_REQUEST(HttpStatus.UNAUTHORIZED, "INVALID_LOGIN_REQUEST", "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "만료된 토큰입니다."),
    PENDING_USER(HttpStatus.FORBIDDEN, "PENDING_USER", "승인 대기 중인 계정입니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "유효하지 않은 역할입니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "DUPLICATE_LOGIN_ID", "이미 사용 중인 사용자 ID입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),

    // Department
    DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DEPARTMENT_NOT_FOUND", "부서를 찾을 수 없습니다."),
    DUPLICATE_DEPARTMENT_NAME(HttpStatus.CONFLICT, "DUPLICATE_DEPARTMENT_NAME", "이미 존재하는 부서명입니다."),
    DEPARTMENT_IN_USE(HttpStatus.CONFLICT, "DEPARTMENT_IN_USE", "사용자가 소속된 부서는 삭제할 수 없습니다."),

    // Patent
    PATENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PATENT_NOT_FOUND", "특허를 찾을 수 없습니다."),
    DUPLICATE_APPLICATION_NUMBER(HttpStatus.CONFLICT, "DUPLICATE_APPLICATION_NUMBER", "이미 등록된 출원번호입니다."),
    PATENT_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PATENT_DOCUMENT_NOT_FOUND", "특허 문서를 찾을 수 없습니다."),
    PATENT_DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PATENT_DEPARTMENT_NOT_FOUND", "특허 담당 부서 정보를 찾을 수 없습니다."),
    PATENT_LEGAL_STATUS_NOT_FOUND(HttpStatus.NOT_FOUND, "PATENT_LEGAL_STATUS_NOT_FOUND", "특허 권리 상태 이력을 찾을 수 없습니다."),
    PATENT_ANNUITY_NOT_FOUND(HttpStatus.NOT_FOUND, "PATENT_ANNUITY_NOT_FOUND", "특허 연차료를 찾을 수 없습니다."),

    // Report
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "보고서를 찾을 수 없습니다."),
    REPORT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "REPORT_GENERATION_FAILED", "보고서 생성에 실패했습니다."),
    REPORT_NOT_COMPLETED(HttpStatus.CONFLICT, "REPORT_NOT_COMPLETED", "아직 생성이 완료되지 않은 보고서입니다."),

    // Review
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "사업부 검토를 찾을 수 없습니다."),
    DUPLICATE_REVIEW_REQUEST(HttpStatus.CONFLICT, "DUPLICATE_REVIEW_REQUEST", "이미 요청된 사업부 검토입니다."),
    OPINION_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "OPINION_ALREADY_SUBMITTED", "이미 의견이 제출된 요청입니다."),
    INVALID_REVIEW_STATUS(HttpStatus.CONFLICT, "INVALID_REVIEW_STATUS", "처리할 수 없는 사업부 검토 상태입니다."),

    // External
    AI_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI_SERVER_ERROR", "AI 서버 연동 중 오류가 발생했습니다."),
    KIPRIS_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "KIPRIS_API_ERROR", "KIPRIS API 연동 중 오류가 발생했습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_UPLOAD_FAILED", "파일 업로드에 실패했습니다."),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_DELETE_FAILED", "파일 삭제에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
