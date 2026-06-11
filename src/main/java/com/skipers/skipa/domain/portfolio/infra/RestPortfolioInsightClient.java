package com.skipers.skipa.domain.portfolio.infra;

import com.skipers.skipa.domain.portfolio.application.PortfolioInsightClient;
import com.skipers.skipa.domain.portfolio.dto.request.PortfolioInsightClientRequest;
import com.skipers.skipa.domain.portfolio.dto.response.PortfolioInsightClientResponse;
import com.skipers.skipa.domain.portfolio.exception.PortfolioException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

@Component
@Profile("!local & !test")
public class RestPortfolioInsightClient implements PortfolioInsightClient {

    private final RestClient restClient;
    private final String insightsPath;

    public RestPortfolioInsightClient(
            @Value("${app.ai-server.base-url}") String baseUrl,
            @Value("${app.ai-server.portfolio-insights-path}") String insightsPath,
            @Value("${app.ai-server.portfolio-insights-connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${app.ai-server.portfolio-insights-read-timeout-ms:30000}") long readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.insightsPath = insightsPath;
    }

    @Override
    public List<String> generate(PortfolioInsightClientRequest request) {
        PortfolioInsightClientResponse response;
        try {
            response = restClient.post()
                    .uri(insightsPath)
                    .body(request)
                    .retrieve()
                    .body(PortfolioInsightClientResponse.class);
        } catch (RestClientException e) {
            throw new PortfolioException(ErrorCode.AI_SERVER_ERROR, e);
        }

        if (response == null || response.insights() == null || response.insights().isEmpty()) {
            throw new PortfolioException(ErrorCode.AI_SERVER_ERROR);
        }

        return response.insights();
    }
}
