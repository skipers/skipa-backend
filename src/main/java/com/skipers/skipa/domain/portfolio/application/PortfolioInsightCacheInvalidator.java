package com.skipers.skipa.domain.portfolio.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioInsightCacheInvalidator {

    private final PortfolioInsightCache portfolioInsightCache;

    public void evict() {
        portfolioInsightCache.evict();
    }
}
