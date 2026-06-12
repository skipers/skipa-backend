package com.skipers.skipa.domain.chat.domain;

import java.util.List;

public record ChatSourceCard(
        String label,
        String title,
        String displayTitle,
        String sourceType,
        Integer pageNo,
        String url,
        String locationLabel,
        String sourcePath,
        List<String> matchTerms,
        String snippet
) {
}
