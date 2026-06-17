package com.skipers.skipa.domain.chat.application;

import java.util.function.Consumer;

public interface ChatStreamClient<T> {

    void stream(T request, Consumer<ChatStreamEvent> eventConsumer);
}
