package com.skipers.skipa.domain.dashboard.dto.response;

import java.util.List;

public record DashboardDistributionResponse(
        List<TechFieldItem> byTechField,
        List<ExpiryQuarterItem> byExpiryQuarter
) {

    public record TechFieldItem(
            String name,
            long count
    ) {
    }

    public record ExpiryQuarterItem(
            String quarter,
            long count
    ) {
    }
}
