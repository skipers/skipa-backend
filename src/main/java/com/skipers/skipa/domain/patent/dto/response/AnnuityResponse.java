package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.AnnuityHistory;
import com.skipers.skipa.domain.patent.domain.AnnuityStatus;

import java.time.Instant;
import java.time.LocalDate;

public record AnnuityResponse(
        Long id, // 납부 이력 ID
        Long patentId, // 특허 ID
        Integer annuityYear, // 연차
        LocalDate dueDate, // 납부 기한
        LocalDate paidDate, // 실제 납부일자
        AnnuityStatus status, // 납부 상태
        Integer amount, // 납부 금액
        Instant createdAt, // 생성일시
        Instant updatedAt // 수정일시
) {

    public static AnnuityResponse from(AnnuityHistory history) { // 엔티티 → 응답 DTO 변환
        return new AnnuityResponse(
                history.getId(),
                history.getPatent().getId(),
                history.getAnnuityYear(),
                history.getDueDate(),
                history.getPaidDate(),
                history.getStatus(),
                history.getAmount(),
                history.getCreatedAt(),
                history.getUpdatedAt()
        );
    }
}
