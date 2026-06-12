package com.skipers.skipa.domain.preevaluation.application;

import com.skipers.skipa.domain.chat.dto.ChatClientResult;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationChatClientRequest;

public interface PreEvaluationChatClient {

    ChatClientResult send(PreEvaluationChatClientRequest request);
}
