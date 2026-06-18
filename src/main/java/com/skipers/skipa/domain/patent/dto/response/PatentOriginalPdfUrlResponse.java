package com.skipers.skipa.domain.patent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "특허 원문 PDF 접근 URL 응답")
public record PatentOriginalPdfUrlResponse(
        @Schema(description = "특허 ID", example = "1")
        Long patentId,

        @Schema(description = "원문 PDF MinIO object key", example = "patents/1/original.pdf")
        String originalPdfKey,

        @Schema(description = "원문 PDF 접근 URL")
        String url
) {
}
