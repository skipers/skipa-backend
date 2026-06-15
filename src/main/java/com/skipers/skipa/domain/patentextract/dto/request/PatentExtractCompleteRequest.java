package com.skipers.skipa.domain.patentextract.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record PatentExtractCompleteRequest(

        @NotBlank(message = "parsedJsonKey는 필수입니다.")
        String parsedJsonKey,

        @NotNull(message = "result는 필수입니다.")
        Map<String, Object> result
) {
}
