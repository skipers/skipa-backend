package com.skipers.skipa.domain.patent.dto.response;

import com.skipers.skipa.domain.patent.domain.Patent;

import java.time.LocalDate;
import java.util.List;

public record ExpiringPatentCalendarResponse(
        List<MonthBucket> months
) {

    public record MonthBucket(
            int month,
            long count,
            List<PatentItem> patents
    ) {
    }

    public record PatentItem(
            Long id,
            String title,
            String applicationNumber,
            LocalDate expiryDate,
            String techField
    ) {

        public static PatentItem from(Patent patent) {
            return new PatentItem(
                    patent.getId(),
                    patent.getTitle(),
                    patent.getApplicationNumber(),
                    patent.getExpiryDate(),
                    patent.getTechField()
            );
        }
    }
}
