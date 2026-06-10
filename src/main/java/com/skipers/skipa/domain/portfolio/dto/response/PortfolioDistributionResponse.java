package com.skipers.skipa.domain.portfolio.dto.response;

import java.util.List;

public record PortfolioDistributionResponse(
        List<GradeDistribution> byGrade,
        List<NameCount> byTechField,
        List<CountryCount> byFilingCountry,
        List<DepartmentCount> byDepartment
) {

    public record GradeDistribution(
            Long departmentId,
            String departmentName,
            long s,
            long a,
            long b,
            long c,
            long d
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
