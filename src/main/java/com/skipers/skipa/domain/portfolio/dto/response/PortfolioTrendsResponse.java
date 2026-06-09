package com.skipers.skipa.domain.portfolio.dto.response;

import java.util.List;

public record PortfolioTrendsResponse(
        List<YearlyPatentTrend> yearlyPatentTrends,
        List<YearlyAnnuityCost> yearlyAnnuityCosts
) {

    public record YearlyPatentTrend(
            int year,
            long applications,
            long registrations,
            long expiries
    ) {
    }

    public record YearlyAnnuityCost(
            int year,
            long amount
    ) {
    }
}
