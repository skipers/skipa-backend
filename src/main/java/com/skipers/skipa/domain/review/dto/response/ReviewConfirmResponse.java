package com.skipers.skipa.domain.review.dto.response;

import com.skipers.skipa.domain.review.domain.Review;

public record ReviewConfirmResponse(
        Long id,
        boolean checked
) {

    public static ReviewConfirmResponse from(Review review) {
        return new ReviewConfirmResponse(
                review.getId(),
                review.isChecked()
        );
    }
}
