package com.skipers.skipa.domain.portfolio.infra;

import com.skipers.skipa.domain.portfolio.application.PortfolioInsightClient;
import com.skipers.skipa.domain.portfolio.dto.request.PortfolioInsightClientRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("local | test")
public class LocalPortfolioInsightClient implements PortfolioInsightClient {

    @Override
    public List<String> generate(PortfolioInsightClientRequest request) {
        return List.of("로컬 환경에서는 AI 포트폴리오 인사이트를 생성하지 않습니다.");
    }
}
