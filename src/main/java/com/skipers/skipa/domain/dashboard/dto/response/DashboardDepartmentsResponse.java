package com.skipers.skipa.domain.dashboard.dto.response;

import java.util.List;

public record DashboardDepartmentsResponse(
        List<Item> items
) {

    public record Item(
            Long departmentId,
            String departmentName,
            long assigned,
            long reviewing,
            long decided,
            int progressRate
    ) {
    }
}
