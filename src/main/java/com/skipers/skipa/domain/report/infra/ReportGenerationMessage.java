package com.skipers.skipa.domain.report.infra;

public record ReportGenerationMessage(
        String type,
        Long reportId,
        Long patentId
) {

    private static final String REPORT_GENERATE = "REPORT_GENERATE";

    public static ReportGenerationMessage of(Long reportId, Long patentId) {
        return new ReportGenerationMessage(REPORT_GENERATE, reportId, patentId);
    }
}
