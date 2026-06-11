package com.skipers.skipa.domain.report.infra;

import com.skipers.skipa.domain.report.application.ReportChatClient;
import com.skipers.skipa.domain.report.dto.request.ReportChatClientRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalReportChatClient implements ReportChatClient {

    @Override
    public String send(ReportChatClientRequest request) {
        return "로컬 환경에서는 AI 서버 평가 보고서 채팅 응답을 생성하지 않습니다.";
    }
}
