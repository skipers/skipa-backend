package com.skipers.skipa.domain.review.domain;

import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.domain.PatentAnnuity;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.report.domain.Report;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reviews_cycle_patent_department",
                        columnNames = {"review_cycle_id", "patent_id", "department_id"}
                )
        }
)
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // 사업부 검토 ID
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 특허(N) : (1) 사업부 검토
    @JoinColumn(name = "patent_id", nullable = false)
    private Patent patent;

    @ManyToOne(fetch = FetchType.LAZY) // 부서(N) : (1) 사업부 검토
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY) // 검토 주기(N) : (1) 사업부 검토
    @JoinColumn(name = "review_cycle_id", nullable = false)
    private ReviewCycle reviewCycle;

    @ManyToOne(fetch = FetchType.LAZY) // 참고 보고서(N) : (1) 사업부 검토
    @JoinColumn(name = "report_id")
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY) // 재평가 대상 연차료(N) : (1) 사업부 검토
    @JoinColumn(name = "patent_annuity_id")
    private PatentAnnuity patentAnnuity;

    @Enumerated(EnumType.STRING)
    @Column(name = "opinion", length = 20) // 사업부 의견(MAINTAIN/ABANDON)
    private BusinessOpinion opinion;

    @Column(name = "comment", columnDefinition = "text") // 의견 코멘트
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20) // 제출 상태(PENDING/SUBMITTED)
    private ReviewStatus status;

    @Column(name = "submitted_at") // 제출일시
    private Instant submittedAt;

    @Column(name = "due_date", nullable = false) // 회신 기한
    private LocalDate dueDate;

    @Column(name = "checked", nullable = false) // Legal 팀 회신 확인 여부
    private boolean checked;

    @Builder
    private Review(
            Patent patent,
            Department department,
            ReviewCycle reviewCycle,
            Report report,
            PatentAnnuity patentAnnuity,
            BusinessOpinion opinion,
            String comment,
            ReviewStatus status,
            Instant submittedAt,
            LocalDate dueDate,
            Boolean checked
    ) {
        this.patent = patent;
        this.department = department;
        this.reviewCycle = reviewCycle;
        this.report = report;
        this.patentAnnuity = patentAnnuity;
        this.opinion = opinion;
        this.comment = comment;
        this.status = status != null ? status : ReviewStatus.PENDING;
        this.submittedAt = submittedAt;
        this.dueDate = dueDate != null ? dueDate : reviewCycle.getEndDate();
        this.checked = checked != null && checked;
    }

    public void request(Report report) {
        this.report = report;
        this.status = ReviewStatus.PENDING;
    }

    public void submit(BusinessOpinion opinion, String comment, Instant submittedAt) {
        this.opinion = opinion;
        this.comment = comment;
        this.status = ReviewStatus.SUBMITTED;
        this.submittedAt = submittedAt;
        this.checked = false;
    }

    public void confirm() {
        this.checked = true;
    }
}
