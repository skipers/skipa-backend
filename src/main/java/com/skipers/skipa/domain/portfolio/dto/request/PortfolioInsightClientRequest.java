package com.skipers.skipa.domain.portfolio.dto.request;

import com.skipers.skipa.domain.portfolio.dto.response.PortfolioDecisionResponse;
import com.skipers.skipa.domain.portfolio.dto.response.PortfolioDistributionResponse;
import com.skipers.skipa.domain.portfolio.dto.response.PortfolioTrendsResponse;

public record PortfolioInsightClientRequest(
        PortfolioTrendsResponse trends,
        PortfolioDistributionResponse distribution,
        PortfolioDecisionResponse decisions
) {
}
