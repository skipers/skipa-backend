package com.skipers.skipa.domain.preevaluation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PreEvaluationCreateRequest(
        @NotBlank(message = "특허명은 필수입니다.")
        @Size(max = 200, message = "특허명은 200자 이하여야 합니다.")
        @Schema(description = "특허명", example = "배터리 열폭주 감지 시스템")
        String title,

        @NotBlank(message = "기술 설명은 필수입니다.")
        @Schema(description = "기술 설명", example = "센서 데이터를 기반으로 배터리 열폭주 가능성을 조기에 감지하는 기술")
        String technicalDescription,

        @Schema(description = "청구항 목록", example = "[\"센서부를 포함하는 배터리 열폭주 감지 시스템\", \"분석부가 센서 데이터를 기반으로 위험도를 산출하는 시스템\"]")
        List<@NotBlank(message = "청구항은 빈 값일 수 없습니다.") String> claims,

        @Size(max = 500, message = "관련 사업은 500자 이하여야 합니다.")
        @Schema(description = "관련 사업", example = "전기차 배터리 안전 관리")
        String relatedBusiness,

        @Size(max = 500, message = "출원 예정 국가는 500자 이하여야 합니다.")
        @Schema(description = "출원 예정 국가", example = "한국, 미국")
        String targetCountries
) {
}
