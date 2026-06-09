package com.skipers.skipa.domain.patent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PatentUpdateRequest(

        @NotBlank(message = "특허명은 필수입니다.")
        @Size(max = 500, message = "특허명은 500자 이하여야 합니다.")
        @Schema(description = "특허명", example = "반도체 패키지 구조(수정)")
        String title,

        @NotBlank(message = "출원번호는 필수입니다.")
        @Size(max = 20, message = "출원번호는 20자 이하여야 합니다.")
        @Schema(description = "출원번호(중복 불가)", example = "10-2026-0000000")
        String applicationNumber,

        @Size(max = 20, message = "등록번호는 20자 이하여야 합니다.")
        @Schema(description = "등록번호", example = "10-1234567")
        String registrationNumber,

        @Size(max = 20, message = "공개번호는 20자 이하여야 합니다.")
        @Schema(description = "공개번호", example = "10-2026-0000001")
        String publicationNumber,

        @Size(max = 20, message = "공고번호는 20자 이하여야 합니다.")
        @Schema(description = "공고번호", example = "10-2026-0000002")
        String announcementNumber,

        @Schema(description = "출원일자", example = "2026-05-26")
        LocalDate applicationDate,

        @Schema(description = "등록일자", example = "2026-05-26")
        LocalDate registrationDate,

        @Schema(description = "공개일자", example = "2026-05-26")
        LocalDate publicationDate,

        @Schema(description = "공고일자", example = "2026-05-26")
        LocalDate announcementDate,

        @Schema(description = "IPC 코드", example = "[\"H01L 21/00\",\"H01L 23/00\"]")
        List<String> ipcCodes,

        @Schema(description = "CPC 코드", example = "[\"H01L 21/00\",\"H01L 23/00\"]")
        List<String> cpcCodes,

        @Size(max = 200, message = "출원인은 200자 이하여야 합니다.")
        @Schema(description = "출원인", example = "SK")
        String applicant,

        @Size(max = 500, message = "발명자는 500자 이하여야 합니다.")
        @Schema(description = "발명자", example = "홍길동")
        String inventor,

        @Schema(description = "예상 소멸일자", example = "2046-05-26")
        LocalDate expiryDate,

        @Schema(description = "피인용 수", example = "10")
        Integer citationCount,

        @Schema(description = "심사청구항수", example = "12")
        Integer examinationClaimCount,

        @Size(max = 500, message = "원문 파일 키는 500자 이하여야 합니다.")
        @Schema(description = "원문 파일 키", example = "patents/original/xxx.pdf")
        String originalPdfKey,

        @Size(max = 50, message = "관리번호는 50자 이하여야 합니다.")
        @Schema(description = "관리번호", example = "MNG-2026-0001")
        String managementNumber,

        @Size(max = 200, message = "관련사업 분야는 200자 이하여야 합니다.")
        @Schema(description = "관련사업 분야", example = "반도체")
        String businessField,

        @Size(max = 200, message = "관련기술 분야는 200자 이하여야 합니다.")
        @Schema(description = "관련기술 분야", example = "패키징")
        String techField,

        @Schema(description = "관련제품", example = "[\"제품A\",\"제품B\"]")
        List<String> relatedProducts,

        @Size(max = 100, message = "출원국가는 100자 이하여야 합니다.")
        @Schema(description = "출원국가", example = "KR")
        String filingCountry,

        @Schema(description = "공동출원 여부", example = "false")
        Boolean isJointApplication,

        @Size(max = 200, message = "공동출원인은 200자 이하여야 합니다.")
        @Schema(description = "공동출원인", example = "협력사A")
        String jointApplicant,

        @Size(max = 200, message = "최초 담당 부서는 200자 이하여야 합니다.")
        @Schema(description = "최초 담당 부서", example = "반도체")
        String initialDepartment,

        @Schema(description = "키워드", example = "[\"패키지\",\"반도체\"]")
        List<String> keywords,

        @Schema(description = "개요", example = "특허 개요")
        String overview,

        @Schema(description = "핵심 내용", example = "특허 핵심 내용")
        String coreContent
) {}
