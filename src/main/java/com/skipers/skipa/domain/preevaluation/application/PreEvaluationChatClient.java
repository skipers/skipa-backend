package com.skipers.skipa.domain.preevaluation.application;

import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationChatClientRequest;

public interface PreEvaluationChatClient {

    String send(PreEvaluationChatClientRequest request);
}
