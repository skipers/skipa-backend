package com.skipers.skipa.domain.review.dto.request;

import java.time.LocalDate;

public record ReviewCreateRequest(
        LocalDate dueDate
) {
}
