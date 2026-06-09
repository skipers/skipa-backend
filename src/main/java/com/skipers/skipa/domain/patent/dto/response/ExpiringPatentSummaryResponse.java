package com.skipers.skipa.domain.patent.dto.response;

import java.util.List;

public record ExpiringPatentSummaryResponse(
        List<PeriodCount> byPeriod,
        List<TechFieldCount> byTechField
) {

    public record PeriodCount(
            int months,
            long count
    ) {
    }

    public record TechFieldCount(
            String name,
            long count
    ) {
    }
}
