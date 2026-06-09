package com.skipers.skipa.domain.dashboard.api;

import com.skipers.skipa.domain.dashboard.application.DashboardService;
import com.skipers.skipa.domain.dashboard.dto.response.BusinessDashboardResponse;
import com.skipers.skipa.domain.dashboard.dto.response.LegalDashboardResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Legal 홈 대시보드 조회", description = "Legal 홈 화면에 필요한 검토 현황, 분포, 최근 회신 정보를 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @GetMapping("/legal")
    public ApiResponse<LegalDashboardResponse> getLegalDashboard(
            @RequestParam(required = false) Long reviewCycleId
    ) {
        return ApiResponse.ok(dashboardService.getLegalDashboard(reviewCycleId));
    }

    @Operation(summary = "Business 홈 대시보드 조회", description = "Business 홈 화면에 필요한 소속 사업부 기준 검토 현황과 특허 요약을 조회합니다.")
    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping("/business")
    public ApiResponse<BusinessDashboardResponse> getBusinessDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long reviewCycleId
    ) {
        return ApiResponse.ok(dashboardService.getBusinessDashboard(userDetails.getUser(), reviewCycleId));
    }
}
