package com.skipers.skipa.domain.report.dto.response;

public record ReportCallbackResponse(
        Long reportId,
        String status
) {

    public static ReportCallbackResponse from(ReportStatusResponse response) {
        return new ReportCallbackResponse(
                response.id(),
                response.status()
        );
    }
}
