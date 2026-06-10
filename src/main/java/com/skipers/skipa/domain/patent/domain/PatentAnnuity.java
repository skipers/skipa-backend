package com.skipers.skipa.domain.patent.domain;

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

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "patent_annuities")
public class PatentAnnuity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // 연차료 납부 이력 ID
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 특허(N) : (1) 매핑
    @JoinColumn(name = "patent_id", nullable = false)
    private Patent patent;

    @Column(name = "start_year", nullable = false) // 납부 시작 연차
    private Integer startYear;

    @Column(name = "end_year") // 납부 종료 연차
    private Integer endYear;

    @Column(name = "due_date") // 납부 기한
    private LocalDate dueDate;

    @Column(name = "paid_date") // 실제 납부일자
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20) // 납부 상태(PAID/UNPAID/ABANDONED)
    private PatentAnnuityStatus status;

    @Column(name = "amount") // 납부 금액
    private Integer amount;

    @Builder
    private PatentAnnuity(
            Patent patent,
            Integer startYear,
            Integer endYear,
            LocalDate dueDate,
            LocalDate paidDate,
            PatentAnnuityStatus status,
            Integer amount
    ) {
        this.patent = patent;
        this.startYear = startYear;
        this.endYear = endYear;
        this.dueDate = dueDate;
        this.paidDate = paidDate;
        this.status = status;
        this.amount = amount;
    }

    public void pay(Integer paymentYears, Integer amount, LocalDate paidDate) {
        this.endYear = this.startYear + paymentYears - 1;
        this.amount = amount;
        this.paidDate = paidDate;
        this.status = PatentAnnuityStatus.PAID;
    }

    public void abandon() {
        this.status = PatentAnnuityStatus.ABANDONED;
    }
}
