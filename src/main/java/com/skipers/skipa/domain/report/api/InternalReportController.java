package com.skipers.skipa.domain.report.api;

import com.skipers.skipa.domain.report.application.ReportService;
import com.skipers.skipa.domain.report.dto.request.ReportCompleteRequest;
import com.skipers.skipa.domain.report.dto.request.ReportFailRequest;
import com.skipers.skipa.domain.report.dto.response.ReportCallbackResponse;
import com.skipers.skipa.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/reports")
public class InternalReportController {

    private final ReportService reportService;

    @PatchMapping("/{reportId}/complete")
    public ApiResponse<ReportCallbackResponse> complete(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportCompleteRequest request
    ) {
        return ApiResponse.ok(ReportCallbackResponse.from(reportService.complete(reportId, request.reportKey())));
    }

    @PatchMapping("/{reportId}/fail")
    public ApiResponse<ReportCallbackResponse> fail(
            @PathVariable Long reportId,
            @RequestBody(required = false) ReportFailRequest request
    ) {
        return ApiResponse.ok(ReportCallbackResponse.from(reportService.fail(reportId)));
    }
}
