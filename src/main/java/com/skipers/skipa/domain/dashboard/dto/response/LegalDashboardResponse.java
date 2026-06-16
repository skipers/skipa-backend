package com.skipers.skipa.domain.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LegalDashboardResponse(
        ReviewCycleSummary reviewCycle,
        Kpi kpi,
        CycleProgress cycleProgress,
        List<DepartmentProgress> departments,
        List<NameCount> byTechField,
        List<QuarterCount> byExpiryQuarter,
        List<RecentReply> recentReplies
) {

    @Schema(name = "LegalDashboardKpi")
    public record Kpi(
            long requested,
            long reviewing,
            long decided,
            long overdue,
            long unread,
            long unrequested
    ) {
    }

    public record DepartmentProgress(
            Long departmentId,
            String departmentName,
            long assigned,
            long decided
    ) {
    }

    public record CycleProgress(
            long targetPatentCount,
            ReportProgress reports,
            ReviewProgress reviews,
            @Schema(description = """
                    Overall cycle progress label.
                    NO_TARGETS: no review targets selected yet.
                    REPORT_NOT_STARTED: at least one target has no report.
                    REPORT_GENERATING: at least one report is being generated.
                    REPORT_FAILED: at least one report generation failed.
                    REVIEW_NOT_REQUESTED: reports are ready but at least one business review is still scheduled.
                    REVIEW_IN_PROGRESS: at least one business review is pending or overdue.
                    REVIEW_COMPLETED: all business reviews have been submitted.
                    """)
            String statusLabel
    ) {
    }

    public record ReportProgress(
            long notStarted,
            long generating,
            long completed,
            long failed
    ) {
    }

    public record ReviewProgress(
            long scheduled,
            long inReview,
            long submitted
    ) {
    }

    public record NameCount(
            String name,
            long count
    ) {
    }

    public record QuarterCount(
            String quarter,
            long count
    ) {
    }

    public record RecentReply(
            Long reviewId,
            Long patentId,
            String title,
            String applicationNumber,
            Long departmentId,
            String departmentName,
            String opinion,
            Instant submittedAt,
            boolean checked,
            LocalDate dueDate
    ) {
    }
}
