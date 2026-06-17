package com.skipers.skipa.domain.chat.application;

import com.fasterxml.jackson.databind.JsonNode;

public record ChatStreamEvent(
        String event,
        JsonNode data,
        String raw
) {
}
