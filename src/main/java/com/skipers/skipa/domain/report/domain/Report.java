package com.skipers.skipa.domain.report.domain;

import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.report.exception.ReportException;
import com.skipers.skipa.global.common.entity.BaseTimeEntity;
import com.skipers.skipa.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reports")
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // 평가 보고서 ID
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 특허(N) : (1) 평가 보고서
    @JoinColumn(name = "patent_id", nullable = false)
    private Patent patent;

    @Column(name = "report_key", length = 500) // 보고서 키(S3 key)
    private String reportKey;

    @Column(name = "total_score", precision = 5, scale = 2) // AI 평가 총점
    private BigDecimal totalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20) // 생성 상태(GENERATING/COMPLETED/FAILED)
    private ReportStatus status;

    @Column(name = "evaluated_at") // 평가 완료 시각
    private Instant evaluatedAt;

    @Builder
    private Report(Patent patent, String reportKey, BigDecimal totalScore, ReportStatus status, Instant evaluatedAt) {
        this.patent = patent;
        this.reportKey = reportKey;
        this.totalScore = totalScore;
        this.status = status != null ? status : ReportStatus.GENERATING;
        this.evaluatedAt = evaluatedAt;
    }

    public void complete(String reportKey, BigDecimal totalScore, Instant evaluatedAt) {
        validateGenerating();

        if (reportKey == null || reportKey.isBlank()) {
            throw new ReportException(ErrorCode.INVALID_REQUEST);
        }
        if (totalScore == null) {
            throw new ReportException(ErrorCode.INVALID_REQUEST);
        }

        this.reportKey = reportKey;
        this.totalScore = totalScore;
        this.status = ReportStatus.COMPLETED;
        this.evaluatedAt = evaluatedAt != null ? evaluatedAt : Instant.now();
    }

    public void fail() {
        validateGenerating();

        this.status = ReportStatus.FAILED;
    }

    public boolean isCompleted() {
        return status == ReportStatus.COMPLETED;
    }

    private void validateGenerating() {
        if (status != ReportStatus.GENERATING) {
            throw new ReportException(ErrorCode.REPORT_ALREADY_PROCESSED);
        }
    }
}
