package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.Patent;

import java.time.LocalDate;

public record ExpiringPatentItemResponse(
        Long id,
        String title,
        String applicationNumber,
        String techField,
        Long departmentId,
        String departmentName,
        LocalDate expiryDate
) {

    public static ExpiringPatentItemResponse from(Patent patent) {
        Long departmentId = patent.getCurrentDepartment() == null ? null : patent.getCurrentDepartment().getId();
        String departmentName = patent.getCurrentDepartment() == null ? null : patent.getCurrentDepartment().getName();

        return new ExpiringPatentItemResponse(
                patent.getId(),
                patent.getTitle(),
                patent.getApplicationNumber(),
                patent.getTechField(),
                departmentId,
                departmentName,
                patent.getExpiryDate()
        );
    }
}
