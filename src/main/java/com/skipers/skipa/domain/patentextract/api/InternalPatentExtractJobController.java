package com.skipers.skipa.domain.patentextract.api;

import com.skipers.skipa.domain.patentextract.application.PatentExtractJobService;
import com.skipers.skipa.domain.patentextract.dto.request.PatentExtractCompleteRequest;
import com.skipers.skipa.domain.patentextract.dto.request.PatentExtractFailRequest;
import com.skipers.skipa.domain.patentextract.dto.response.PatentExtractCallbackResponse;
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
@RequestMapping("/internal/patent-extract-jobs")
public class InternalPatentExtractJobController {

    private final PatentExtractJobService patentExtractJobService;

    @PatchMapping("/{extractJobId}/complete")
    public ApiResponse<PatentExtractCallbackResponse> complete(
            @PathVariable Long extractJobId,
            @Valid @RequestBody PatentExtractCompleteRequest request
    ) {
        return ApiResponse.ok(PatentExtractCallbackResponse.from(
                patentExtractJobService.complete(extractJobId, request.result())
        ));
    }

    @PatchMapping("/{extractJobId}/fail")
    public ApiResponse<PatentExtractCallbackResponse> fail(
            @PathVariable Long extractJobId,
            @RequestBody(required = false) PatentExtractFailRequest request
    ) {
        String errorMessage = request != null ? request.errorMessage() : null;
        return ApiResponse.ok(PatentExtractCallbackResponse.from(
                patentExtractJobService.fail(extractJobId, errorMessage)
        ));
    }
}
