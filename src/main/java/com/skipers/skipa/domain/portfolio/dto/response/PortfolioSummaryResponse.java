package com.skipers.skipa.domain.portfolio.dto.response;

import java.util.List;

public record PortfolioSummaryResponse(
        long totalPatents,
        long expiringWithinYear,
        long countryCount,
        long techFieldCount,
        List<String> insights
) {
}
