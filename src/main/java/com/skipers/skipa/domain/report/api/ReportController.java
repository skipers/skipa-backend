package com.skipers.skipa.domain.report.api;

import com.skipers.skipa.domain.report.application.ReportService;
import com.skipers.skipa.domain.report.dto.response.ReportCreateResponse;
import com.skipers.skipa.domain.report.dto.response.ReportResponse;
import com.skipers.skipa.domain.report.dto.response.ReportStatusResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.response.PageResponse;
import com.skipers.skipa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/patents/{patentId}/reports")
public class ReportController {

    private final ReportService reportService;

    /**
     * 특허의 평가 보고서 생성을 요청한다.
     *
     * @param patentId 특허 ID
     * @return 생성 요청된 평가 보고서
     */
    @Operation(summary = "평가 보고서 생성 요청", description = "특허의 평가 보고서 생성을 요청합니다.")
    @PreAuthorize("hasRole('LEGAL')")
    @PostMapping
    public ResponseEntity<ApiResponse<ReportCreateResponse>> create(
            @PathVariable Long patentId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(reportService.create(patentId)));
    }

    /**
     * 특허의 평가 보고서 목록을 조회한다(page/size 기반).
     *
     * @param userDetails 인증 사용자 정보
     * @param patentId 특허 ID
     * @param pageable page/size 정보
     * @return 평가 보고서 목록 페이지
     */
    @Operation(summary = "평가 보고서 목록 조회", description = "특허의 평가 보고서 목록을 페이지 단위로 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @GetMapping
    public ApiResponse<PageResponse<ReportResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long patentId,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(reportService.getAll(userDetails.getUser(), patentId, pageable)));
    }

    /**
     * 특허의 평가 보고서를 ID로 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param patentId 특허 ID
     * @param reportId 평가 보고서 ID
     * @return 평가 보고서
     */
    @Operation(summary = "평가 보고서 단일 조회", description = "평가 보고서 ID로 상세 정보를 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @GetMapping("/{reportId}")
    public ApiResponse<ReportResponse> get(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long patentId,
            @PathVariable Long reportId
    ) {
        return ApiResponse.ok(reportService.get(userDetails.getUser(), patentId, reportId));
    }

    /**
     * 특허의 평가 보고서 생성 상태를 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param patentId 특허 ID
     * @param reportId 평가 보고서 ID
     * @return 평가 보고서 생성 상태
     */
    @Operation(summary = "평가 보고서 생성 상태 조회", description = "평가 보고서의 생성 상태를 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @GetMapping("/{reportId}/status")
    public ApiResponse<ReportStatusResponse> getStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long patentId,
            @PathVariable Long reportId
    ) {
        return ApiResponse.ok(reportService.getStatus(userDetails.getUser(), patentId, reportId));
    }
}
