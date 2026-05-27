package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.Patent;

import java.time.Instant;

public record PatentListResponse(
        Long id, // 특허 ID
        String title, // 특허명
        String applicationNumber, // 출원번호
        String applicant, // 출원인
        String inventor, // 발명자
        Instant createdAt, // 생성일시
        Instant updatedAt // 수정일시
) {

    public static PatentListResponse from(Patent patent) { // 엔티티 → 목록 응답 DTO 변환
        return new PatentListResponse(
                patent.getId(),
                patent.getTitle(),
                patent.getApplicationNumber(),
                patent.getApplicant(),
                patent.getInventor(),
                patent.getCreatedAt(),
                patent.getUpdatedAt()
        );
    }
}

