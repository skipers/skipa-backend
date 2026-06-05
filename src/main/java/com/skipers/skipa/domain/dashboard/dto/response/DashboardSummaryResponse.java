package com.skipers.skipa.domain.dashboard.dto.response;

public record DashboardSummaryResponse(
        int progressRate,
        long delayed,
        Kpi kpi
) {

    public record Kpi(
            long requested,
            long reviewing,
            long decided
    ) {
    }
}
