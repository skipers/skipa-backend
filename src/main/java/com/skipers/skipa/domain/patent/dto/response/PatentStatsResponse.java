package com.skipers.skipa.domain.patent.dto.response;

import java.util.List;
import java.util.Map;

public record PatentStatsResponse(
        long total,
        Map<String, Long> byLegalStatus,
        ExpiringStats expiring,
        List<NameCount> byTechField,
        List<QuarterCount> byExpiryQuarter,
        List<CountryCount> byFilingCountry,
        List<DepartmentCount> byDepartment
) {

    public record ExpiringStats(
            long in3Months,
            long in6Months,
            long in1Year
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
