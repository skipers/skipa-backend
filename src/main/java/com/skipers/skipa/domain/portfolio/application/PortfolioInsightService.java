package com.skipers.skipa.domain.portfolio.application;

import com.skipers.skipa.domain.portfolio.dto.request.PortfolioInsightClientRequest;
import com.skipers.skipa.domain.portfolio.dto.response.PortfolioInsightsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioInsightService {

    private final PortfolioService portfolioService;
    private final PortfolioInsightClient portfolioInsightClient;
    private final PortfolioInsightCache portfolioInsightCache;

    @Value("${app.portfolio.insights-cache-ttl-seconds:86400}")
    private long insightsCacheTtlSeconds;

    public PortfolioInsightsResponse getInsights() {
        return portfolioInsightCache.get()
                .map(PortfolioInsightsResponse::new)
                .orElseGet(this::generateInsights);
    }

    private PortfolioInsightsResponse generateInsights() {
        PortfolioInsightClientRequest request = new PortfolioInsightClientRequest(
                portfolioService.getTrends(),
                portfolioService.getDistribution(),
                portfolioService.getDecisions()
        );
        List<String> insights = portfolioInsightClient.generate(request);
        portfolioInsightCache.put(insights, Duration.ofSeconds(insightsCacheTtlSeconds));
        return new PortfolioInsightsResponse(insights);
    }
}
