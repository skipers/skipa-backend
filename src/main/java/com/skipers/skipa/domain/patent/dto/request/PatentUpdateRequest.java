package com.skipers.skipa.domain.patent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record PatentUpdateRequest(

        @NotBlank(message = "특허명은 필수입니다.") // 필수 입력값 검증
        @Schema(description = "특허명", example = "반도체 패키지 구조(수정)") // Swagger 문서화
        String title,

        @Schema(description = "등록번호", example = "10-1234567") // 선택 입력값
        String registrationNumber,

        @Schema(description = "공개번호", example = "10-2026-0000001") // 선택 입력값
        String publicationNumber,

        @Schema(description = "공고번호", example = "10-2026-0000002") // 선택 입력값
        String announcementNumber,

        @Schema(description = "출원일자", example = "2026-05-26") // 선택 입력값
        LocalDate applicationDate,

        @Schema(description = "등록일자", example = "2026-05-26") // 선택 입력값
        LocalDate registrationDate,

        @Schema(description = "공개일자", example = "2026-05-26") // 선택 입력값
        LocalDate publicationDate,

        @Schema(description = "공고일자", example = "2026-05-26") // 선택 입력값
        LocalDate announcementDate,

        @Schema(description = "IPC 코드", example = "H01L 21/00") // 선택 입력값
        String ipcCode,

        @Schema(description = "CPC 코드", example = "H01L 21/00") // 선택 입력값
        String cpcCode,

        @Schema(description = "출원인", example = "SK") // 선택 입력값
        String applicant,

        @Schema(description = "발명자", example = "홍길동") // 선택 입력값
        String inventor,

        @Schema(description = "예상 소멸일자", example = "2046-05-26") // 선택 입력값
        LocalDate expiryDate,

        @Schema(description = "피인용 수", example = "10") // 선택 입력값
        Integer citationCount,

        @Schema(description = "원문 파일 키", example = "patents/original/xxx.pdf") // 선택 입력값
        String originalPdfKey,

        @Schema(description = "관리번호", example = "MNG-2026-0001") // 선택 입력값
        String managementNumber,

        @Schema(description = "관련사업 분야", example = "반도체") // 선택 입력값
        String businessField,

        @Schema(description = "관련기술 분야", example = "패키징") // 선택 입력값
        String techField,

        @Schema(description = "관련제품(JSON 문자열)", example = "[\"제품A\",\"제품B\"]") // 선택 입력값
        String relatedProducts,

        @Schema(description = "출원국가", example = "KR") // 선택 입력값
        String filingCountry,

        @Schema(description = "공동출원 여부", example = "false") // 선택 입력값
        Boolean isJointApplication,

        @Schema(description = "공동출원인", example = "협력사A") // 선택 입력값
        String jointApplicant,

        @Schema(description = "최초 담당 부서", example = "반도체") // 선택 입력값
        String initialDepartment,

        @Schema(description = "키워드(JSON 문자열)", example = "[\"패키지\",\"반도체\"]") // 선택 입력값
        String keywords,

        @Schema(description = "개요", example = "특허 개요") // 선택 입력값
        String overview,

        @Schema(description = "핵심 내용", example = "특허 핵심 내용") // 선택 입력값
        String coreContent
) {}

