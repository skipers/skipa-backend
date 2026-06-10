package com.skipers.skipa.domain.patent.dto.response;

import java.util.List;

public record ExpiringPatentSummaryResponse(
        List<PeriodTechFieldCount> periods
) {

    public record PeriodTechFieldCount(
            Integer months,
            List<TechFieldCount> byTechField
    ) {
    }

    public record TechFieldCount(
            String name,
            long count
    ) {
    }
}
