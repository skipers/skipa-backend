package com.skipers.skipa.domain.portfolio.dto.response;

import java.util.List;

public record PortfolioDistributionResponse(
        List<GradeCount> byValueGrade,
        List<NameCount> byTechField,
        List<CountryCount> byFilingCountry,
        List<DepartmentCount> byDepartment
) {

    public record GradeCount(
            String grade,
            long count
    ) {
    }

    public record NameCount(
            String name,
            long count
    ) {
    }

    public record CountryCount(
            String country,
            long count
    ) {
    }

    public record DepartmentCount(
            Long departmentId,
            String departmentName,
            long count
    ) {
    }
}
