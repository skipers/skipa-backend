package com.skipers.skipa.domain.dashboard.api;

import com.skipers.skipa.domain.dashboard.application.DashboardService;
import com.skipers.skipa.domain.dashboard.dto.response.DashboardAssignmentResponse;
import com.skipers.skipa.domain.dashboard.dto.response.DashboardDepartmentsResponse;
import com.skipers.skipa.domain.dashboard.dto.response.DashboardDistributionResponse;
import com.skipers.skipa.domain.dashboard.dto.response.DashboardSummaryResponse;
import com.skipers.skipa.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "재평가 진행 현황", description = "재평가 대시보드 KPI 요약 정보를 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> getSummary() {
        return ApiResponse.ok(dashboardService.getSummary());
    }

    @Operation(summary = "담당 부서 배정 현황", description = "특허의 담당 부서 배정 현황 요약 정보를 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @GetMapping("/assignment")
    public ApiResponse<DashboardAssignmentResponse> getAssignment() {
        return ApiResponse.ok(dashboardService.getAssignment());
    }

    @Operation(summary = "특허 유형 분포 / 만료 현황", description = "기술 분야 분포와 분기별 만료 예정 특허 현황을 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @GetMapping("/distribution")
    public ApiResponse<DashboardDistributionResponse> getDistribution() {
        return ApiResponse.ok(dashboardService.getDistribution());
    }

    @Operation(summary = "사업부별 검토 현황", description = "사업부별 검토 요청 현황과 진행률을 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @GetMapping("/departments")
    public ApiResponse<DashboardDepartmentsResponse> getDepartments() {
        return ApiResponse.ok(dashboardService.getDepartments());
    }
}
