package com.skipers.skipa.domain.review.dto.response;

import java.util.List;

public record ReviewStatsResponse(
        Long reviewCycleId,
        String reviewCycleName,
        long total,
        long unassigned,
        long requested,
        long overdue,
        long done,
        long unread,
        long maintain,
        long abandon,
        double progressRate,
        List<DepartmentStats> byDepartment,
        List<TechFieldStats> byTechField
) {

    public record DepartmentStats(
            Long departmentId,
            String departmentName,
            long maintain,
            long abandon
    ) {
    }

    public record TechFieldStats(
            String name,
            long maintain,
            long abandon
    ) {
    }
}
