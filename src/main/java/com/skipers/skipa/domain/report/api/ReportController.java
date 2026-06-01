package com.skipers.skipa.domain.report.api;

import com.skipers.skipa.domain.report.application.ReportService;
import com.skipers.skipa.domain.report.dto.request.ReportCreateRequest;
import com.skipers.skipa.domain.report.dto.response.ReportCreateResponse;
import com.skipers.skipa.domain.report.dto.response.ReportResponse;
import com.skipers.skipa.domain.report.dto.response.ReportStatusResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/patents/{patentId}/reports") // 평가 보고서
public class ReportController {

    private final ReportService reportService;

    @PreAuthorize("hasRole('LEGAL')")
    @PostMapping
    public ResponseEntity<ApiResponse<ReportCreateResponse>> create(
            @PathVariable Long patentId,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(reportService.create(patentId, request)));
    }

    @PreAuthorize("hasAnyRole('LEGAL', 'BUSINESS')")
    @GetMapping
    public ApiResponse<PageResponse<ReportResponse>> getAll(
            @PathVariable Long patentId,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(reportService.getAll(patentId, pageable)));
    }

    @PreAuthorize("hasAnyRole('LEGAL', 'BUSINESS')")
    @GetMapping("/{reportId}")
    public ApiResponse<ReportResponse> get(
            @PathVariable Long patentId,
            @PathVariable Long reportId
    ) {
        return ApiResponse.ok(reportService.get(patentId, reportId));
    }

    @PreAuthorize("hasAnyRole('LEGAL', 'BUSINESS')")
    @GetMapping("/{reportId}/status")
    public ApiResponse<ReportStatusResponse> getStatus(
            @PathVariable Long patentId,
            @PathVariable Long reportId
    ) {
        return ApiResponse.ok(reportService.getStatus(patentId, reportId));
    }
}

