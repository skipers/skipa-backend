package com.skipers.skipa.domain.report.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ReportCompleteRequest(

        @NotBlank(message = "reportKey는 필수입니다.")
        String reportKey,

        @NotNull(message = "totalScore는 필수입니다.")
        @DecimalMin(value = "0.00", message = "totalScore는 0 이상이어야 합니다.")
        @DecimalMax(value = "100.00", message = "totalScore는 100 이하여야 합니다.")
        @Digits(integer = 3, fraction = 2, message = "totalScore는 정수 3자리, 소수 2자리까지 허용됩니다.")
        BigDecimal totalScore
) {
}
