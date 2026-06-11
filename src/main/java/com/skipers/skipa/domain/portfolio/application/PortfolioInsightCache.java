package com.skipers.skipa.domain.portfolio.application;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface PortfolioInsightCache {

    Optional<List<String>> get();

    void put(List<String> insights, Duration ttl);

    void evict();
}
