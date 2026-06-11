package com.skipers.skipa.domain.portfolio.api;

import com.skipers.skipa.domain.portfolio.application.PortfolioInsightService;
import com.skipers.skipa.domain.portfolio.application.PortfolioService;
import com.skipers.skipa.domain.portfolio.dto.response.PortfolioDecisionResponse;
import com.skipers.skipa.domain.portfolio.dto.response.PortfolioDistributionResponse;
import com.skipers.skipa.domain.portfolio.dto.response.PortfolioInsightsResponse;
import com.skipers.skipa.domain.portfolio.dto.response.PortfolioTrendsResponse;
import com.skipers.skipa.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioInsightService portfolioInsightService;

    /**
     * 포트폴리오 인사이트를 조회한다.
     *
     * @return 포트폴리오 인사이트
     */
    @Operation(summary = "[Legal] 포트폴리오 인사이트 조회", description = "포트폴리오 추이, 분포, 결정 비율 데이터를 기반으로 AI가 생성한 인사이트를 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @GetMapping("/insights")
    public ApiResponse<PortfolioInsightsResponse> getInsights() {
        return ApiResponse.ok(portfolioInsightService.getInsights());
    }

    /**
     * 포트폴리오 분포를 조회한다.
     *
     * @return 포트폴리오 분포
     */
    @Operation(summary = "[Legal] 포트폴리오 분포 조회", description = "전체 및 사업부별 평가 등급 분포와 기술 분야, 국가, 사업부별 포트폴리오 분포를 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @GetMapping("/distribution")
    public ApiResponse<PortfolioDistributionResponse> getDistribution() {
        return ApiResponse.ok(portfolioService.getDistribution());
    }

    /**
     * 포트폴리오 추이를 조회한다.
     *
     * @return 포트폴리오 추이
     */
    @Operation(summary = "[Legal] 포트폴리오 추이 조회", description = "연도별 출원/등록/소멸 추이와 연차료 추이를 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @GetMapping("/trends")
    public ApiResponse<PortfolioTrendsResponse> getTrends() {
        return ApiResponse.ok(portfolioService.getTrends());
    }

    /**
     * 포트폴리오 유지/포기 결정 현황을 조회한다.
     *
     * @return 포트폴리오 유지/포기 결정 현황
     */
    @Operation(summary = "[Legal] 포트폴리오 결정 비율 조회", description = "분기, 사업부, 기술 분야별 유지/포기 결정 현황을 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @GetMapping("/decisions")
    public ApiResponse<PortfolioDecisionResponse> getDecisions() {
        return ApiResponse.ok(portfolioService.getDecisions());
    }
}
