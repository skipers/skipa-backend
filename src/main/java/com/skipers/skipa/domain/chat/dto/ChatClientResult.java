package com.skipers.skipa.domain.chat.dto;

import com.skipers.skipa.domain.chat.domain.ChatSourceCard;

import java.util.List;

public record ChatClientResult(
        String answer,
        List<ChatSourceCard> sourceCards
) {

    public ChatClientResult {
        sourceCards = sourceCards != null ? sourceCards : List.of();
    }

    public static ChatClientResult answerOnly(String answer) {
        return new ChatClientResult(answer, List.of());
    }
}
