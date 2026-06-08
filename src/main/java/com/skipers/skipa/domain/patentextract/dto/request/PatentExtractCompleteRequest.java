package com.skipers.skipa.domain.patentextract.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record PatentExtractCompleteRequest(

        @NotNull(message = "result는 필수입니다.")
        JsonNode result
) {
}
