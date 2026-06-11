package com.skipers.skipa.domain.report.application;

import com.skipers.skipa.domain.report.dto.request.ReportChatClientRequest;

public interface ReportChatClient {

    String send(ReportChatClientRequest request);
}
