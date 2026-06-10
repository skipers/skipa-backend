package com.skipers.skipa.domain.preevaluation.api;

import com.skipers.skipa.domain.preevaluation.application.PreEvaluationService;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationCompleteRequest;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationFailRequest;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationCallbackResponse;
import com.skipers.skipa.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/pre-evaluations")
public class InternalPreEvaluationController {

    private final PreEvaluationService preEvaluationService;

    @PatchMapping("/{preEvaluationId}/complete")
    public ApiResponse<PreEvaluationCallbackResponse> complete(
            @PathVariable Long preEvaluationId,
            @Valid @RequestBody PreEvaluationCompleteRequest request
    ) {
        return ApiResponse.ok(PreEvaluationCallbackResponse.from(
                preEvaluationService.complete(preEvaluationId, request.reportUrl())
        ));
    }

    @PatchMapping("/{preEvaluationId}/fail")
    public ApiResponse<PreEvaluationCallbackResponse> fail(
            @PathVariable Long preEvaluationId,
            @RequestBody(required = false) PreEvaluationFailRequest request
    ) {
        return ApiResponse.ok(PreEvaluationCallbackResponse.from(preEvaluationService.fail(preEvaluationId)));
    }
}
