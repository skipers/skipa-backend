package com.skipers.skipa.domain.chat.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.chat.application.ChatStreamEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AiChatStreamClientSupport<T> {

    private final HttpClient httpClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    protected AiChatStreamClientSupport(
            String baseUrl,
            long connectTimeoutMs,
            ObjectMapper objectMapper
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.objectMapper = objectMapper;
    }

    protected void stream(T request, String path, Consumer<ChatStreamEvent> eventConsumer) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofMillis(readTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AI stream server returned status " + response.statusCode());
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                consumeEvents(reader, eventConsumer);
            }
        } catch (IOException e) {
            throw new IllegalStateException("AI stream server communication failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI stream server communication interrupted", e);
        }
    }

    protected long readTimeoutMs() {
        return 120_000;
    }

    private void consumeEvents(BufferedReader reader, Consumer<ChatStreamEvent> eventConsumer) throws IOException {
        String event = "message";
        List<String> dataLines = new ArrayList<>();
        StringBuilder raw = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                emitEvent(event, dataLines, raw, eventConsumer);
                event = "message";
                dataLines = new ArrayList<>();
                raw = new StringBuilder();
                continue;
            }

            raw.append(line).append('\n');
            if (line.startsWith(":")) {
                continue;
            }
            if (line.startsWith("event:")) {
                event = line.substring("event:".length()).trim();
            } else if (line.startsWith("data:")) {
                dataLines.add(line.substring("data:".length()).trim());
            }
        }

        emitEvent(event, dataLines, raw, eventConsumer);
    }

    private void emitEvent(
            String event,
            List<String> dataLines,
            StringBuilder raw,
            Consumer<ChatStreamEvent> eventConsumer
    ) throws JsonProcessingException {
        if (raw.isEmpty()) {
            return;
        }
        String rawEvent = raw.append('\n').toString();
        String data = String.join("\n", dataLines);
        JsonNode dataNode = data.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(data);
        eventConsumer.accept(new ChatStreamEvent(event, dataNode, rawEvent));
    }

    private String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
