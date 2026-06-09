package com.skipers.skipa.domain.patent.dto.response;

import java.util.List;

public record ExpiringPatentCalendarResponse(
        List<MonthBucket> months
) {

    public record MonthBucket(
            int year,
            int month,
            long count,
            List<ExpiringPatentItemResponse> items
    ) {
    }
}
