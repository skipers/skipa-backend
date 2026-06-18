package com.skipers.skipa.domain.preevaluation.domain;

import com.skipers.skipa.domain.preevaluation.exception.PreEvaluationException;
import com.skipers.skipa.domain.user.domain.User;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "pre_evaluations")
public class PreEvaluation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "technical_description", columnDefinition = "text", nullable = false)
    private String technicalDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "claims")
    private List<String> claims;

    @Column(name = "related_business", length = 500)
    private String relatedBusiness;

    @Column(name = "target_countries", length = 500)
    private String targetCountries;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PreEvaluationStatus status;

    @Column(name = "report_key", length = 500)
    private String reportKey;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder
    private PreEvaluation(
            User user,
            String title,
            String technicalDescription,
            List<String> claims,
            String relatedBusiness,
            String targetCountries,
            PreEvaluationStatus status,
            String reportKey,
            Instant completedAt
    ) {
        this.user = user;
        this.title = title;
        this.technicalDescription = technicalDescription;
        this.claims = claims;
        this.relatedBusiness = relatedBusiness;
        this.targetCountries = targetCountries;
        this.status = status != null ? status : PreEvaluationStatus.PROCESSING;
        this.reportKey = reportKey;
        this.completedAt = completedAt;
    }

    public void completeReport(String reportKey, Instant completedAt) {
        validateProcessing();

        if (reportKey == null || reportKey.isBlank()) {
            throw new PreEvaluationException(ErrorCode.INVALID_REQUEST);
        }

        this.reportKey = reportKey;
        this.status = PreEvaluationStatus.REPORT_COMPLETED;
        this.completedAt = completedAt != null ? completedAt : Instant.now();
    }

    public void completeEmbedding() {
        if (status != PreEvaluationStatus.REPORT_COMPLETED) {
            throw new PreEvaluationException(ErrorCode.PRE_EVALUATION_ALREADY_PROCESSED);
        }

        this.status = PreEvaluationStatus.EMBEDDING_COMPLETED;
    }

    public void fail() {
        validateProcessing();

        this.status = PreEvaluationStatus.FAILED;
        this.completedAt = Instant.now();
    }

    public boolean isReportGenerated() {
        return status == PreEvaluationStatus.REPORT_COMPLETED || status == PreEvaluationStatus.EMBEDDING_COMPLETED;
    }

    private void validateProcessing() {
        if (status != PreEvaluationStatus.PROCESSING) {
            throw new PreEvaluationException(ErrorCode.PRE_EVALUATION_ALREADY_PROCESSED);
        }
    }
}
