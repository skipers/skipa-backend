package com.skipers.skipa.domain.review.domain;

import com.skipers.skipa.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "review_cycles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_review_cycles_year_quarter", columnNames = {"cycle_year", "quarter"})
        }
)
public class ReviewCycle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // 검토 주기 ID
    private Long id;

    @Column(name = "cycle_year", nullable = false) // 검토 주기 연도
    private Integer year;

    @Column(name = "quarter", nullable = false) // 검토 주기 분기
    private Integer quarter;

    @Column(name = "start_date", nullable = false) // 시작일
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false) // 종료일
    private LocalDate endDate;

    @Builder
    private ReviewCycle(Integer year, Integer quarter, LocalDate startDate, LocalDate endDate) {
        this.year = year;
        this.quarter = quarter;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void update(Integer year, Integer quarter, LocalDate startDate, LocalDate endDate) {
        this.year = year;
        this.quarter = quarter;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
