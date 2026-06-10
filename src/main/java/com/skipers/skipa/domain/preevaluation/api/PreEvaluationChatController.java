package com.skipers.skipa.domain.preevaluation.api;

import com.skipers.skipa.domain.preevaluation.application.PreEvaluationChatService;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationChatMessageRequest;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationChatMessageResponse;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationChatSendResponse;
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
@RequestMapping("/pre-evaluations/{preEvaluationId}/chat/messages")
public class PreEvaluationChatController {

    private final PreEvaluationChatService preEvaluationChatService;

    @Operation(summary = "[Business] 사전 평가 채팅 이력 조회", description = "현재 사용자의 사전 평가 채팅 메시지 이력을 조회합니다.")
    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping
    public ApiResponse<List<PreEvaluationChatMessageResponse>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long preEvaluationId
    ) {
        return ApiResponse.ok(preEvaluationChatService.getMessages(userDetails.getUser(), preEvaluationId));
    }

    @Operation(summary = "[Business] 사전 평가 채팅 메시지 전송", description = "사용자 메시지를 저장하고 AI 서버 채팅 API를 호출한 뒤 응답을 저장합니다.")
    @PreAuthorize("hasRole('BUSINESS')")
    @PostMapping
    public ResponseEntity<ApiResponse<PreEvaluationChatSendResponse>> sendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long preEvaluationId,
            @Valid @RequestBody PreEvaluationChatMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(preEvaluationChatService.sendMessage(
                        userDetails.getUser(),
                        preEvaluationId,
                        request
                )));
    }

    @Operation(summary = "[Business] 사전 평가 채팅 초기화", description = "현재 사용자의 사전 평가 채팅 메시지를 모두 삭제합니다.")
    @PreAuthorize("hasRole('BUSINESS')")
    @DeleteMapping
    public ApiResponse<Void> clearMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long preEvaluationId
    ) {
        preEvaluationChatService.clearMessages(userDetails.getUser(), preEvaluationId);
        return ApiResponse.ok();
    }
}
