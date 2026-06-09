package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.Patent;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record ExpiringPatentItemResponse(
        Long id,
        String title,
        String applicationNumber,
        String techField,
        Long departmentId,
        String departmentName,
        LocalDate expiryDate,
        long daysUntilExpiry
) {

    public static ExpiringPatentItemResponse from(Patent patent, LocalDate today) {
        Long departmentId = patent.getCurrentDepartment() == null ? null : patent.getCurrentDepartment().getId();
        String departmentName = patent.getCurrentDepartment() == null ? null : patent.getCurrentDepartment().getName();

        return new ExpiringPatentItemResponse(
                patent.getId(),
                patent.getTitle(),
                patent.getApplicationNumber(),
                patent.getTechField(),
                departmentId,
                departmentName,
                patent.getExpiryDate(),
                ChronoUnit.DAYS.between(today, patent.getExpiryDate())
        );
    }
}
