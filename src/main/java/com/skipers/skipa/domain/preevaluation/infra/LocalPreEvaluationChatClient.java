package com.skipers.skipa.domain.preevaluation.infra;

import com.skipers.skipa.domain.preevaluation.application.PreEvaluationChatClient;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationChatClientRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalPreEvaluationChatClient implements PreEvaluationChatClient {

    @Override
    public String send(PreEvaluationChatClientRequest request) {
        return "로컬 환경에서는 AI 서버 채팅 응답을 생성하지 않습니다.";
    }
}
