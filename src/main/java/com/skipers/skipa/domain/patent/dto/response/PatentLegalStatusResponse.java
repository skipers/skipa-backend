package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.PatentLegalStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatusType;

import java.time.Instant;
import java.time.LocalDate;

public record PatentLegalStatusResponse(
        Long id, // 이력 ID
        Long patentId, // 특허 ID
        PatentLegalStatusType status, // 권리 상태
        LocalDate changedAt, // 상태 변경일자
        Instant createdAt, // 생성일시
        Instant updatedAt // 수정일시
) {

    public static PatentLegalStatusResponse from(PatentLegalStatus history) { // 엔티티 → 응답 DTO 변환
        return new PatentLegalStatusResponse(
                history.getId(),
                history.getPatent().getId(),
                history.getStatus(),
                history.getChangedAt(),
                history.getCreatedAt(),
                history.getUpdatedAt()
        );
    }
}
