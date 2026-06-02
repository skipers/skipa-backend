package com.skipers.skipa.domain.review.dto.response;

import java.util.List;

public record BulkReviewCreateResponse(
        Long reviewCycleId,
        int createdCount,
        int skippedCount,
        List<Item> items
) {

    public record Item(
            Long patentId,
            String status,
            String reason
    ) {

        public static Item created(Long patentId) {
            return new Item(patentId, "CREATED", null);
        }

        public static Item skipped(Long patentId, String reason) {
            return new Item(patentId, "SKIPPED", reason);
        }
    }
}
