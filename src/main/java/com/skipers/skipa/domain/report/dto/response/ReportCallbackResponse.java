package com.skipers.skipa.domain.report.dto.response;

import java.math.BigDecimal;

public record ReportCallbackResponse(
        Long reportId,
        String status,
        BigDecimal totalScore
) {

    public static ReportCallbackResponse from(ReportStatusResponse response) {
        return new ReportCallbackResponse(
                response.id(),
                response.status(),
                response.totalScore()
        );
    }
}
