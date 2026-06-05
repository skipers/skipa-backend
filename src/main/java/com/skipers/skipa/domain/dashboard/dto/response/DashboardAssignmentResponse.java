package com.skipers.skipa.domain.dashboard.dto.response;

public record DashboardAssignmentResponse(
        long unassigned,
        long assigned,
        long completed
) {
}
