package com.skipers.skipa.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SerializationAndOpenApiConfigTest {

    @Test
    void jacksonConfigSerializesJavaTimesAsIsoStrings() throws Exception {
        ObjectMapper objectMapper = new JacksonConfig().objectMapper();

        String json = objectMapper.writeValueAsString(Map.of(
                "createdAt",
                Instant.parse("2026-05-26T00:00:00Z")
        ));

        assertThat(json).contains("\"createdAt\":\"2026-05-26T00:00:00Z\"");
    }

    @Test
    void openApiConfigProvidesApplicationMetadata() {
        OpenAPI openAPI = new OpenApiConfig().openAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("SKIPA API");
        assertThat(openAPI.getInfo().getDescription()).isEqualTo("SKIPA backend API documentation");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1.0.0");
    }
}
