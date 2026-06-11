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

    /**
     * AI 서버의 평가 보고서 생성 완료 콜백을 처리한다.
     *
     * @param reportId 평가 보고서 ID
     * @param request 완료 콜백 요청
     * @return 완료 처리된 평가 보고서 상태
     */
    @PatchMapping({"/{reportId}/complete", "/{reportId}/report-complete"})
    public ApiResponse<ReportCallbackResponse> complete(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportCompleteRequest request
    ) {
        return ApiResponse.ok(ReportCallbackResponse.from(reportService.complete(
                reportId,
                request.reportKey(),
                request.totalScore(),
                request.valueGrade()
        )));
    }

    /**
     * AI 서버의 평가 보고서 임베딩 완료 콜백을 처리한다.
     *
     * @param reportId 평가 보고서 ID
     * @return 임베딩 완료 처리된 평가 보고서 상태
     */
    @PatchMapping("/{reportId}/embedding-complete")
    public ApiResponse<ReportCallbackResponse> completeEmbedding(
            @PathVariable Long reportId
    ) {
        return ApiResponse.ok(ReportCallbackResponse.from(reportService.completeEmbedding(reportId)));
    }

    /**
     * AI 서버의 평가 보고서 생성 실패 콜백을 처리한다.
     *
     * @param reportId 평가 보고서 ID
     * @param request 실패 콜백 요청
     * @return 실패 처리된 평가 보고서 상태
     */
    @PatchMapping("/{reportId}/fail")
    public ApiResponse<ReportCallbackResponse> fail(
            @PathVariable Long reportId,
            @RequestBody(required = false) ReportFailRequest request
    ) {
        return ApiResponse.ok(ReportCallbackResponse.from(reportService.fail(reportId)));
    }
}
