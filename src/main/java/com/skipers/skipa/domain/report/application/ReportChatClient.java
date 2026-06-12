package com.skipers.skipa.domain.report.application;

import com.skipers.skipa.domain.chat.dto.ChatClientResult;
import com.skipers.skipa.domain.report.dto.request.ReportChatClientRequest;

public interface ReportChatClient {

    ChatClientResult send(ReportChatClientRequest request);
}
