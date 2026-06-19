package com.skipers.skipa.domain.portfolio.infra;

import com.skipers.skipa.domain.portfolio.application.PortfolioInsightCache;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@Profile("local | local-postgres | test")
public class InMemoryPortfolioInsightCache implements PortfolioInsightCache {

    private List<String> insights;
    private Instant expiresAt;

    @Override
    public Optional<List<String>> get() {
        if (insights == null || expiresAt == null || Instant.now().isAfter(expiresAt)) {
            return Optional.empty();
        }
        return Optional.of(insights);
    }

    @Override
    public void put(List<String> insights, Duration ttl) {
        this.insights = List.copyOf(insights);
        this.expiresAt = Instant.now().plus(ttl);
    }

    @Override
    public void evict() {
        this.insights = null;
        this.expiresAt = null;
    }
}
