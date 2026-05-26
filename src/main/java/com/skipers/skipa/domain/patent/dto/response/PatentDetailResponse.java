package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.Patent;

import java.time.Instant;
import java.time.LocalDate;

public record PatentDetailResponse(
        Long id, // 특허 ID
        String title, // 특허명
        String applicationNumber, // 출원번호
        String registrationNumber, // 등록번호
        String publicationNumber, // 공개번호
        String announcementNumber, // 공고번호
        LocalDate applicationDate, // 출원일자
        LocalDate registrationDate, // 등록일자
        LocalDate publicationDate, // 공개일자
        LocalDate announcementDate, // 공고일자
        String ipcCode, // IPC 코드
        String cpcCode, // CPC 코드
        String applicant, // 출원인
        String inventor, // 발명자
        LocalDate expiryDate, // 예상 소멸일자
        Integer citationCount, // 피인용 수
        String originalPdfKey, // 원문 파일 키
        String managementNumber, // 관리번호
        String businessField, // 관련사업 분야
        String techField, // 관련기술 분야
        String relatedProducts, // 관련제품(JSON 문자열)
        String filingCountry, // 출원국가
        Boolean isJointApplication, // 공동출원 여부
        String jointApplicant, // 공동출원인
        String initialDepartment, // 최초 담당 부서
        String keywords, // 키워드(JSON 문자열)
        String overview, // 개요
        String coreContent, // 핵심 내용
        Instant createdAt, // 생성일시
        Instant updatedAt // 수정일시
) {

    public static PatentDetailResponse from(Patent patent) { // 엔티티 → 응답 DTO 변환
        return new PatentDetailResponse(
                patent.getId(),
                patent.getTitle(),
                patent.getApplicationNumber(),
                patent.getRegistrationNumber(),
                patent.getPublicationNumber(),
                patent.getAnnouncementNumber(),
                patent.getApplicationDate(),
                patent.getRegistrationDate(),
                patent.getPublicationDate(),
                patent.getAnnouncementDate(),
                patent.getIpcCode(),
                patent.getCpcCode(),
                patent.getApplicant(),
                patent.getInventor(),
                patent.getExpiryDate(),
                patent.getCitationCount(),
                patent.getOriginalPdfKey(),
                patent.getManagementNumber(),
                patent.getBusinessField(),
                patent.getTechField(),
                patent.getRelatedProducts(),
                patent.getFilingCountry(),
                patent.getIsJointApplication(),
                patent.getJointApplicant(),
                patent.getInitialDepartment(),
                patent.getKeywords(),
                patent.getOverview(),
                patent.getCoreContent(),
                patent.getCreatedAt(),
                patent.getUpdatedAt()
        );
    }
}

