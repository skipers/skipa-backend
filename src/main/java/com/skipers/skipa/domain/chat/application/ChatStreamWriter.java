package com.skipers.skipa.domain.chat.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class ChatStreamWriter {

    private ChatStreamWriter() {
    }

    public static void write(OutputStream outputStream, String rawEvent) throws IOException {
        outputStream.write(rawEvent.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    public static void writeError(OutputStream outputStream, ObjectMapper objectMapper, String message) throws IOException {
        write(outputStream, errorEvent(objectMapper, message));
    }

    public static String errorEvent(ObjectMapper objectMapper, String message) throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(Map.of(
                "code", "AI_STREAM_ERROR",
                "message", message
        ));
        return "event: error\n" + "data: " + payload + "\n\n";
    }
}
