package com.skipers.skipa.domain.portfolio.application;

import com.skipers.skipa.domain.portfolio.dto.request.PortfolioInsightClientRequest;

import java.util.List;

public interface PortfolioInsightClient {

    List<String> generate(PortfolioInsightClientRequest request);
}
