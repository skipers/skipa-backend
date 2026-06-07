package com.skipers.skipa.domain.report.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReportCompleteRequest(

        @NotBlank(message = "reportKey는 필수입니다.")
        String reportKey
) {
}
