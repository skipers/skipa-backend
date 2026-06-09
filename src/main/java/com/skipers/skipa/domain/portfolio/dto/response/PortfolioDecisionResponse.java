package com.skipers.skipa.domain.portfolio.dto.response;

import java.util.List;

public record PortfolioDecisionResponse(
        List<QuarterDecision> byQuarter,
        List<DepartmentDecision> byDepartment,
        List<TechFieldDecision> byTechField
) {

    public record QuarterDecision(
            String quarter,
            long maintain,
            long abandon
    ) {
    }

    public record DepartmentDecision(
            Long departmentId,
            String departmentName,
            long maintain,
            long abandon
    ) {
    }

    public record TechFieldDecision(
            String name,
            long maintain,
            long abandon
    ) {
    }
}
