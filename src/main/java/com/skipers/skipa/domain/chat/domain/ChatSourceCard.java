package com.skipers.skipa.domain.chat.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatSourceCard(
        String label,
        String title,
        @JsonProperty("display_title")
        String displayTitle,
        @JsonProperty("source_type")
        String sourceType,
        @JsonProperty("page_no")
        Integer pageNo,
        String url,
        @JsonProperty("location_label")
        String locationLabel,
        @JsonProperty("source_path")
        String sourcePath,
        @JsonProperty("match_terms")
        List<String> matchTerms,
        String snippet
) {
}
