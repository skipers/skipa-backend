package com.skipers.skipa.domain.report.api;

import com.skipers.skipa.domain.report.application.ReportChatService;
import com.skipers.skipa.domain.report.dto.request.ReportChatMessageRequest;
import com.skipers.skipa.domain.report.dto.response.ReportChatMessageResponse;
import com.skipers.skipa.domain.report.dto.response.ReportChatSendResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/patents/{patentId}/reports/{reportId}/chat/messages")
public class ReportChatController {

    private final ReportChatService reportChatService;

    @Operation(summary = "[Common] 평가 보고서 채팅 이력 조회", description = "평가 보고서 채팅 메시지 이력을 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @GetMapping
    public ApiResponse<List<ReportChatMessageResponse>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long patentId,
            @PathVariable Long reportId
    ) {
        return ApiResponse.ok(reportChatService.getMessages(userDetails.getUser(), patentId, reportId));
    }

    @Operation(summary = "[Common] 평가 보고서 채팅 메시지 전송", description = "사용자 메시지를 저장하고 AI 서버 채팅 API를 호출한 뒤 응답을 저장합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @PostMapping
    public ResponseEntity<ApiResponse<ReportChatSendResponse>> sendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long patentId,
            @PathVariable Long reportId,
            @Valid @RequestBody ReportChatMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(reportChatService.sendMessage(
                        userDetails.getUser(),
                        patentId,
                        reportId,
                        request
                )));
    }

    @Operation(summary = "[Common] 평가 보고서 채팅 초기화", description = "평가 보고서 채팅 메시지를 모두 삭제합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @DeleteMapping
    public ApiResponse<Void> clearMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long patentId,
            @PathVariable Long reportId
    ) {
        reportChatService.clearMessages(userDetails.getUser(), patentId, reportId);
        return ApiResponse.ok();
    }
}
