package com.skipers.skipa.domain.patentextract.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record PatentExtractCompleteRequest(

        @NotNull(message = "result는 필수입니다.")
        Map<String, Object> result
) {
}
