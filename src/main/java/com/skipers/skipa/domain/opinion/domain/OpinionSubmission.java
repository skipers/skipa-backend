package com.skipers.skipa.domain.opinion.domain;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.global.common.entity.BaseTimeEntity;
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

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "opinion_submissions")
public class OpinionSubmission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // 의견 제출 ID
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 특허(N) : (1) 의견 제출
    @JoinColumn(name = "patent_id", nullable = false)
    private Patent patent;

    @ManyToOne(fetch = FetchType.LAZY) // 부서(N) : (1) 의견 제출
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(name = "opinion", length = 20) // 사업부 의견(유지/포기)
    private BusinessOpinion opinion;

    @Column(name = "comment", columnDefinition = "text") // 의견 코멘트
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20) // 제출 상태(대기/제출완료)
    private OpinionSubmissionStatus status;

    @Column(name = "submitted_at") // 제출일시
    private Instant submittedAt;

    @Builder
    private OpinionSubmission(
            Patent patent,
            Department department,
            BusinessOpinion opinion,
            String comment,
            OpinionSubmissionStatus status,
            Instant submittedAt
    ) {
        this.patent = patent;
        this.department = department;
        this.opinion = opinion;
        this.comment = comment;
        this.status = status != null ? status : OpinionSubmissionStatus.대기;
        this.submittedAt = submittedAt;
    }

    public void submit(BusinessOpinion opinion, String comment, Instant submittedAt) {
        this.opinion = opinion;
        this.comment = comment;
        this.status = OpinionSubmissionStatus.제출완료;
        this.submittedAt = submittedAt;
    }
}
